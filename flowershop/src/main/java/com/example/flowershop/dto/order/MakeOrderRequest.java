package com.example.flowershop.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class MakeOrderRequest {

    @NotBlank(message = "shopId không được để trống")
    private String shopId;

    @NotEmpty(message = "Danh sách cartItemIds không được trống")
    private List<String> cartItemIds;

    @NotBlank(message = "deliveryAddressId không được để trống")
    private String deliveryAddressId;

    // Tuỳ chọn
    private String couponId;

    // Phương thức thanh toán: "COD" hoặc "ONLINE" (mặc định ONLINE nếu null)
    private String paymentMethod;

    // Họ và tên người nhận & số điện thoại nhận hàng
    private String recipientName;
    @Size(max = 20)
    private String phone;
}

