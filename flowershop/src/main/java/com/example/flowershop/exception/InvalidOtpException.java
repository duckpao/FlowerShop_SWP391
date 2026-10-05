package com.example.flowershop.exception;
public class InvalidOtpException extends IllegalArgumentException {
    public InvalidOtpException() {
        super("Mã otp không khớp");
    }
}
