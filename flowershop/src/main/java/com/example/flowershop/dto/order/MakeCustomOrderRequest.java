package com.example.flowershop.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Tạo đơn đặt cọc từ yêu cầu hoa tùy chỉnh đã được shop báo giá: chỉ cần địa chỉ và người nhận. */
@Data
public class MakeCustomOrderRequest {

    @NotBlank(message = "deliveryAddressId không được để trống")
    private String deliveryAddressId;

    @Size(max = 255)
    private String recipientName;

    @Size(max = 20)
    private String phone;
}
