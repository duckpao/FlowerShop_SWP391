package com.example.flowershop.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** Đặt nhiều đơn cùng lúc: mỗi shop một đơn, dùng chung địa chỉ nhận, người nhận và phương thức thanh toán. */
@Data
public class MakeOrdersRequest {

    @NotBlank(message = "deliveryAddressId không được để trống")
    private String deliveryAddressId;

    private String paymentMethod;
    private String recipientName;
    @Size(max = 20)
    private String phone;

    @Valid
    @NotEmpty(message = "Danh sách shop không được trống")
    private List<ShopOrder> shops;

    @Data
    public static class ShopOrder {
        @NotBlank(message = "shopId không được để trống")
        private String shopId;
        @NotEmpty(message = "Danh sách cartItemIds không được trống")
        private List<String> cartItemIds;
        private String couponId;
    }
}
