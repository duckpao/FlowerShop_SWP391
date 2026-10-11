package com.example.flowershop.exception;
public class CaptchaRequiredException extends InvalidOtpException {
    @Override public String getMessage() { return "Vui lòng nhập mã CAPTCHA trong ảnh để tiếp tục."; }
}
