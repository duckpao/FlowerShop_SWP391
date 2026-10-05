package com.example.flowershop.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentResponse {

    private String checkoutUrl;
    private String paymentId;
    private String invoiceNumber;
    private String orderId;
    private BigDecimal amount;
    private Map<String, String> fields;
}

