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
    // Lombok sinh getter isPaid() nên Jackson mặc định xuất "paid"; frontend đọc "isPaid".
    @com.fasterxml.jackson.annotation.JsonProperty("isPaid")
    private boolean isPaid;
    private BigDecimal amount;
    private BigDecimal paidAmount;
    private String paymentType;
    private String transactionNo;
    private String message;
    private LocalDateTime paidAt;
    private String orderType;
    private BigDecimal totalAmount;
    /** Tiền cọc yêu cầu (đã quy đổi: depositAmount > 0 hoặc 50% tổng). */
    private BigDecimal depositAmount;
    private BigDecimal totalPaid;
    private BigDecimal remainingAmount;
    /** "DEPOSIT" | "FINAL" | null: lần thanh toán tiếp theo frontend cần gọi. */
    private String nextPaymentType;
    /** Thanh toán gộp: mọi đơn thuộc giao dịch (orderId là đơn đầu tiên). */
    private java.util.List<String> orderIds;
}
