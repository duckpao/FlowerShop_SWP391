package com.example.flowershop.dto.payment;

import com.example.flowershop.entity.enums.PaymentType;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentRequest {

    @NotBlank(message = "Mã đơn hàng không được để trống")
    private String orderId;

    private PaymentType paymentType; // FULL, DEPOSIT, FINAL

    private String paymentMethod;    // BANK_TRANSFER, CREDIT_CARD, NAPAS (null = cho khách chọn trên cổng)

    private String userId;           // Khách hàng thực hiện thanh toán
}

