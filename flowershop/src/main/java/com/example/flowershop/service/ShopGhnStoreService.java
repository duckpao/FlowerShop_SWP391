package com.example.flowershop.service;

import com.example.flowershop.entity.Address;
import com.example.flowershop.entity.Shop;
import com.example.flowershop.exception.ApiException;
import com.example.flowershop.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Mỗi shop có một cửa hàng GHN riêng, đăng ký bằng địa chỉ lấy hàng của shop. */
@Service
@RequiredArgsConstructor
public class ShopGhnStoreService {

    private final GhnService ghn;
    private final ShopRepository shops;

    /** Mã cửa hàng GHN của shop; chưa có thì đăng ký ngay. */
    // noRollbackFor: lỗi GHN không được đánh dấu rollback-only cho transaction bên ngoài (saveAddress bắt lỗi này).
    @Transactional(noRollbackFor = RuntimeException.class)
    public Integer ensureStore(Shop shop, Address pickup) {
        return shop.getGhnShopId() != null ? shop.getGhnShopId() : registerStore(shop, pickup);
    }

    /** Đăng ký cửa hàng GHN mới theo địa chỉ lấy hàng hiện tại (gọi lại khi shop đổi địa chỉ). */
    // noRollbackFor: lỗi GHN không được đánh dấu rollback-only cho transaction bên ngoài (saveAddress bắt lỗi này).
    @Transactional(noRollbackFor = RuntimeException.class)
    public Integer registerStore(Shop shop, Address pickup) {
        if (pickup.getGhnDistrictId() == null || pickup.getGhnWardCode() == null || pickup.getGhnWardCode().isBlank())
            throw ApiException.badRequest("Địa chỉ lấy hàng của shop chưa có mã GHN.");
        String phone = shop.getOwner() != null ? shop.getOwner().getPhone() : null;
        if (phone == null || phone.isBlank()) phone = pickup.getRecipientPhone();
        if (phone == null || phone.isBlank())
            throw ApiException.badRequest("Cần số điện thoại chủ shop để tạo cửa hàng trên GHN.");
        String address = Stream.of(pickup.getAddressLine(), pickup.getWard(), pickup.getDistrict(), pickup.getCity())
                .filter(Objects::nonNull).filter(part -> !part.isBlank()).collect(Collectors.joining(", "));
        int storeId = ghn.registerStore(shop.getName(), phone, address, pickup.getGhnDistrictId(), pickup.getGhnWardCode());
        shop.setGhnShopId(storeId);
        shops.save(shop);
        return storeId;
    }
}
