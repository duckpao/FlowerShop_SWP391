package com.example.flowershop.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class MakeOrderRequest {

    @NotBlank(message = "shopId không được để trống")
    private String shopId;

    @NotEmpty(message = "Danh sách cartItemIds không được trống")
    private List<String> cartItemIds;

    @NotBlank(message = "deliveryAddressId không được để trống")
    private String deliveryAddressId;

    // Tuỳ chọn
    private String couponId;

    // Phương thức thanh toán: "COD" hoặc "ONLINE" (mặc định ONLINE nếu null)
    private String paymentMethod;

    // Họ và tên người nhận & số điện thoại nhận hàng
    @Size(max = 100, message = "Tên người nhận tối đa 100 ký tự.")
    private String recipientName;
    @Pattern(regexp = "^(?:|0[0-9]{9})$", message = "Số điện thoại gồm 10 chữ số, bắt đầu bằng 0.")
    private String phone;

}

