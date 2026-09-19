package com.example.flowershop.exception;
public class InvalidOtpException extends IllegalArgumentException {
    public InvalidOtpException() {
        super("OTP không hợp lệ, đã dùng, hết hạn hoặc vượt giới hạn nhập sai. Vui lòng thử lại sau.");
    }
}
