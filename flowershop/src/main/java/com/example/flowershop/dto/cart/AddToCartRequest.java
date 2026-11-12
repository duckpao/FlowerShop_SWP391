package com.example.flowershop.dto.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddToCartRequest {

    @NotBlank(message = "productId không được để trống")
    private String productId;

    @NotNull(message = "quantity không được để trống")
    @Min(value = 1, message = "quantity phải ít nhất là 1")
    private Integer quantity;
}

