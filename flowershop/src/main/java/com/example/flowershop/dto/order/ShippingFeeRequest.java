package com.example.flowershop.dto.order;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ShippingFeeRequest {
    @NotBlank(message = "shopId không được để trống")
    private String shopId;

    @NotBlank(message = "deliveryAddressId không được để trống")
    private String deliveryAddressId;

}
