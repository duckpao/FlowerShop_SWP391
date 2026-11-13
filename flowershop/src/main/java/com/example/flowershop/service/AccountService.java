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
    private final ShopRepository shops;

    public AccountService(UserRepository users, AddressRepository addresses, OrderRepository orders,
            CloudinaryService cloudinary, ShopRepository shops) {
        this.users = users;
        this.addresses = addresses;
        this.orders = orders;
        this.cloudinary = cloudinary;
        this.shops = shops;
    }

    // Với chủ shop, ảnh đại diện và logo shop là một: luôn đồng bộ hai chiều.
    private void syncShopLogo(User user, String url) {
        if (user.getRole() != com.example.flowershop.entity.enums.UserRole.SHOP) return;
        shops.findByOwnerIdOrderByNameAsc(user.getId()).forEach(s -> {
            s.setLogoUrl(url);
            s.setLastModifyBy(user.getId());
        });
    }

    @Transactional(readOnly = true)
    public ProfileResponse profile(String userId) {
        return ProfileResponse.from(users.findById(userId).orElseThrow(AccountService::notFound));
    }

    public ProfileResponse updateProfile(String userId, ProfileRequest request) {
        User user = lock(userId);
        user.setFullName(request.fullName().strip());
        String phone = request.phone().strip();
        if (!phone.isEmpty() && users.existsByPhoneAndIdNot(phone, userId))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Số điện thoại đã được dùng bởi tài khoản khác.");
        user.setPhone(phone.isEmpty() ? null : phone); // NULL để cột phone được phép unique
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
                syncShopLogo(user, user.getAvatarUrl());
                return ProfileResponse.from(user);
            } finally { reader.dispose(); }
        } catch (java.io.IOException e) {
            throw new IllegalArgumentException("Không thể đọc ảnh. Vui lòng chọn ảnh JPG hoặc PNG hợp lệ.");
        }
    }

    public ProfileResponse removeAvatar(String userId) {
        User user = lock(userId);
        user.setAvatarUrl(null);
        user.setLastModifyBy(userId);
        syncShopLogo(user, "");
        return ProfileResponse.from(user);
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
            // Đơn chưa có vận đơn vẫn nhận hoa theo địa chỉ đã sửa; chỉ khóa khi có đơn đã tạo vận đơn / đang giao / đã xong.
            if (orders.existsShippedByDeliveryAddressId(id))
                throw lockedByShipment();
        }
        String previousRecipientName = address.getRecipientName();
        String previousRecipientPhone = address.getRecipientPhone();
        address.setAddressLine(request.addressLine().strip());
        address.setCity(request.city().strip());
        address.setDistrict(request.district().strip());
        address.setWard(request.ward().strip());
        address.setGhnDistrictId(request.ghnDistrictId());
        address.setGhnWardCode(request.ghnWardCode().strip());
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
        Address saved = addresses.save(address);
        if (id != null) followRecipientChange(saved, previousRecipientName, previousRecipientPhone, userId);
        return AddressResponse.from(saved);
    }

    /**
     * Người nhận của đơn được chép từ địa chỉ lúc đặt hàng. Khi sửa tên/SĐT người nhận của địa chỉ, các đơn chưa có vận đơn
     * đang dùng đúng giá trị cũ đó được cập nhật theo; giá trị khách đã tự nhập riêng cho đơn thì giữ nguyên.
     */
    private void followRecipientChange(Address address, String previousName, String previousPhone, String actor) {
        for (Order order : orders.findNotShippedByDeliveryAddressId(address.getId())) {
            boolean changed = false;
            if (Objects.equals(order.getRecipientName(), previousName)
                    && !Objects.equals(previousName, address.getRecipientName())) {
                order.setRecipientName(address.getRecipientName());
                changed = true;
            }
            if (Objects.equals(order.getRecipientPhone(), previousPhone)
                    && !Objects.equals(previousPhone, address.getRecipientPhone())) {
                order.setRecipientPhone(address.getRecipientPhone());
                changed = true;
            }
            if (changed) order.setLastModifyBy(actor);
        }
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

    /** Xóa: mọi đơn đang trỏ tới địa chỉ này (kể cả chưa giao) đều cần địa chỉ nhận, nên không xóa được. */
    private static ResponseStatusException inUse() {
        return new ResponseStatusException(HttpStatus.CONFLICT,
                "Địa chỉ đang được dùng trong đơn hàng nên không thể xóa. Bạn vẫn có thể sửa địa chỉ nếu đơn chưa giao cho đơn vị vận chuyển.");
    }

    /** Sửa: chỉ bị chặn khi có đơn đã tạo vận đơn hoặc đã giao. */
    private static ResponseStatusException lockedByShipment() {
        return new ResponseStatusException(HttpStatus.CONFLICT,
                "Địa chỉ đang dùng cho đơn đã tạo vận đơn hoặc đã giao nên không thể sửa. Hãy thêm địa chỉ mới.");
    }
}
