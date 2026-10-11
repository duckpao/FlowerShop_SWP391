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
    private final DeliveryAreaService areas;

    public AccountService(UserRepository users, AddressRepository addresses, OrderRepository orders,
            DeliveryAreaService areas) {
        this.users = users;
        this.addresses = addresses;
        this.orders = orders;
        this.areas = areas;
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
        address.setDistrict("");
        address.setWard(request.ward().strip());
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
