package com.example.flowershop.dto.payment;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CreateGroupPaymentRequest {

    @NotEmpty(message = "Danh sách đơn hàng không được trống")
    private List<String> orderIds;

    private String paymentMethod; // CARD, BANK_TRANSFER, NAPAS_BANK_TRANSFER (null = cho khách chọn trên cổng)
}
