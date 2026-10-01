package com.example.flowershop.dto.payment;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class SePayCheckoutFormResponse {

    private String checkoutUrl;          // Endpoint gửi form: https://pay-sandbox.sepay.vn/v1/checkout/init
    private Map<String, String> fields;   // Toàn bộ input ẩn gồm merchant, order_amount, signature,...
}

