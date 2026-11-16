package com.example.flowershop.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Tạo đơn đặt cọc từ yêu cầu hoa tùy chỉnh đã được shop báo giá: chỉ cần địa chỉ và người nhận. */
@Data
public class MakeCustomOrderRequest {

    @NotBlank(message = "deliveryAddressId không được để trống")
    private String deliveryAddressId;

    @Size(max = 100, message = "Tên người nhận tối đa 100 ký tự.")
    private String recipientName;

    @Pattern(regexp = "^(?:|0[0-9]{9})$", message = "Số điện thoại gồm 10 chữ số, bắt đầu bằng 0.")
    private String phone;

    /** Mã giảm giá (tùy chọn), chỉ áp lúc chấp nhận báo giá; theo luật 1 mã/khách. */
    @Size(max = 50)
    private String couponId;
}
