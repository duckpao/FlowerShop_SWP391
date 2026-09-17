package com.example.flowershop.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatusResponse {
    private String paymentId;
    private String orderId;
    private String invoiceNumber;
    private String orderStatus;
    private String paymentStatus;
    private boolean isPaid;
    private BigDecimal amount;
    private BigDecimal paidAmount;
    private String paymentType;
    private String transactionNo;
    private String message;
    private LocalDateTime paidAt;
}
