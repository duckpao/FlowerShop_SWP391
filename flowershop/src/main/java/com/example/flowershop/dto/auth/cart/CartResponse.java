package com.example.flowershop.dto.cart;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class CartResponse {

    private List<CartItemResponse> items;
    private int totalItems;          // tổng số loại sản phẩm
    private int totalQuantity;       // tổng số lượng
    private BigDecimal grandTotal;   // tổng tiền
}

