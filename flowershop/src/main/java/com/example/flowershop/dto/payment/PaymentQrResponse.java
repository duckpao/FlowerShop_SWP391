package com.example.flowershop.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentQrResponse {
    private String orderId;
    private String orderCode;
    private BigDecimal amount;
    private String bankId;
    private String accountNumber;
    private String accountName;
    private String transferContent;
    private String qrUrl;
    private String paymentStatus;
}

