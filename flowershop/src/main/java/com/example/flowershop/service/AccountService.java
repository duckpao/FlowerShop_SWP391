package com.example.flowershop.service;

import com.example.flowershop.dto.account.*;
import com.example.flowershop.entity.*;
import com.example.flowershop.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Service
@Transactional
public class AccountService {
    private final UserRepository users;
    private final AddressRepository addresses;
    private final OrderRepository orders;
    private final CloudinaryService cloudinary;

    public AccountService(UserRepository users, AddressRepository addresses, OrderRepository orders,
            CloudinaryService cloudinary) {
        this.users = users;
        this.addresses = addresses;
        this.orders = orders;
        this.cloudinary = cloudinary;
    }

    @Transactional(readOnly = true)
    public ProfileResponse profile(String userId) {
        return ProfileResponse.from(users.findById(userId).orElseThrow(AccountService::notFound));
    }

    public ProfileResponse updateProfile(String userId, ProfileRequest request) {
        User user = lock(userId);
        user.setFullName(request.fullName().strip());
        user.setPhone(request.phone().strip());
        user.setLastModifyBy(userId);
        return ProfileResponse.from(user);
    }

    public ProfileResponse uploadAvatar(String userId, org.springframework.web.multipart.MultipartFile file) {
        User user = lock(userId);
        if (file.isEmpty() || file.getSize() > 5 * 1024 * 1024)
            throw new IllegalArgumentException("Ảnh đại diện phải có dung lượng từ 1 byte đến 5 MB.");
        if (!Set.of("image/jpeg", "image/png").contains(file.getContentType() == null ? "" : file.getContentType()))
            throw new IllegalArgumentException("Chỉ hỗ trợ ảnh JPG hoặc PNG.");
        try (var input = javax.imageio.ImageIO.createImageInputStream(file.getInputStream())) {
            var readers = javax.imageio.ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IllegalArgumentException("Tệp tải lên không phải ảnh hợp lệ.");
            var reader = readers.next();
            try {
                reader.setInput(input);
                if (!Set.of("JPEG", "PNG").contains(reader.getFormatName().toUpperCase(Locale.ROOT)))
                    throw new IllegalArgumentException("Chỉ hỗ trợ ảnh JPG hoặc PNG.");
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width < 1 || height < 1 || width > 4096 || height > 4096)
                    throw new IllegalArgumentException("Kích thước ảnh tối đa 4096 × 4096 pixel.");
                var image = reader.read(0);
                var normalized = new java.io.ByteArrayOutputStream();
                javax.imageio.ImageIO.write(image, "png", normalized);
                user.setAvatarUrl(cloudinary.uploadAvatar(normalized.toByteArray()));
                user.setLastModifyBy(userId);
                return ProfileResponse.from(user);
            } finally { reader.dispose(); }
        } catch (java.io.IOException e) {
            throw new IllegalArgumentException("Không thể đọc ảnh. Vui lòng chọn ảnh JPG hoặc PNG hợp lệ.");
        }
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> list(String userId) {
        return owned(userId).stream().map(AddressResponse::from).toList();
    }

    public AddressResponse save(String userId, String id, AddressRequest request) {
        User user = lock(userId); // Serialize all address mutations for one owner, including defaults.
        List<Address> list = owned(userId);
        Address address;
        if (id == null) {
            if (list.size() >= 10)
                throw new IllegalArgumentException("Chỉ được lưu tối đa 10 địa chỉ.");
            address = Address.builder().id(UUID.randomUUID().toString()).user(user).createdBy(userId).build();
        } else {
            address = find(list, id);
            if (orders.existsByDeliveryAddressId(id))
                throw inUse();
        }
        address.setAddressLine(request.addressLine().strip());
        address.setCity(request.city().strip());
        address.setDistrict(request.district() == null ? "" : request.district().strip());
        address.setWard(request.ward().strip());
        address.setGhnWardCode(request.ghnWardCode());
        address.setGhnDistrictId(request.ghnDistrictId());
        address.setRecipientName(request.recipientName().strip());
        address.setRecipientPhone(request.recipientPhone().strip());
        address.setAddressType(request.addressType());
        if (Boolean.TRUE.equals(request.isPickup())) list.forEach(a -> a.setIsPickup(false));
        if (Boolean.TRUE.equals(request.isReturn())) list.forEach(a -> a.setIsReturn(false));
        address.setIsPickup(Boolean.TRUE.equals(request.isPickup()));
        address.setIsReturn(Boolean.TRUE.equals(request.isReturn()));
        address.setLastModifyBy(userId);
        boolean makeDefault = request.isDefault() || list.isEmpty() || Boolean.TRUE.equals(address.getIsDefault())
                || list.stream().noneMatch(a -> Boolean.TRUE.equals(a.getIsDefault()));
        if (makeDefault)
            list.forEach(a -> {
                a.setIsDefault(false);
                a.setLastModifyBy(userId);
            });
        address.setIsDefault(makeDefault);
        return AddressResponse.from(addresses.save(address));
    }

    public AddressResponse setDefault(String userId, String id) {
        lock(userId);
        var list = owned(userId);
        var selected = find(list, id);
        list.forEach(a -> {
            a.setIsDefault(a.getId().equals(id));
            a.setLastModifyBy(userId);
        });
        return AddressResponse.from(selected);
    }

    public void delete(String userId, String id) {
        lock(userId);
        var list = owned(userId);
        var selected = find(list, id);
        if (orders.existsByDeliveryAddressId(id))
            throw inUse();
        addresses.delete(selected);
        if (Boolean.TRUE.equals(selected.getIsDefault())) {
            list.stream().filter(a -> !a.getId().equals(id)).findFirst().ifPresent(a -> {
                a.setIsDefault(true);
                a.setLastModifyBy(userId);
            });
        }
    }

    private User lock(String id) {
        return users.findByIdForUpdate(id).orElseThrow(AccountService::notFound);
    }

    private List<Address> owned(String id) {
        return addresses.findByUserIdAndShopIsNullOrderByCreatedDateAscIdAsc(id);
    }

    private static Address find(List<Address> list, String id) {
        return list.stream().filter(a -> a.getId().equals(id)).findFirst().orElseThrow(AccountService::notFound);
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy dữ liệu.");
    }

    private static ResponseStatusException inUse() {
        return new ResponseStatusException(HttpStatus.CONFLICT,
                "Địa chỉ đã được dùng trong đơn hàng. Hãy thêm địa chỉ mới.");
    }
}
