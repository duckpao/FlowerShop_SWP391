package com.example.flowershop.exception;
public class CaptchaRequiredException extends InvalidOtpException {
    @Override public String getMessage() { return "Bạn đã nhập sai 5 lần. Vui lòng xác minh CAPTCHA để tiếp tục."; }
}
