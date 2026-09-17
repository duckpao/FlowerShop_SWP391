package com.example.flowershop.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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
}

