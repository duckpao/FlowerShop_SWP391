package com.example.flowershop.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/** A merchandise preview does not require a delivery address or reserve vouchers. */
public record CartVoucherPreviewRequest(
        @NotBlank String shopId, @NotEmpty List<String> cartItemIds,
        String deliveryAddressId, @Size(max = 50) String couponId,
        @Size(max = 50) String shippingCouponCode, boolean merchandiseOnly) {
    public MakeOrderRequest toOrderRequest() {
        var request = new MakeOrderRequest();
        request.setShopId(shopId);
        request.setCartItemIds(cartItemIds);
        request.setDeliveryAddressId(deliveryAddressId);
        request.setCouponId(couponId);
        request.setShippingCouponCode(shippingCouponCode);
        return request;
    }
}
