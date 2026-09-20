package com.example.flowershop.service;
public record AuthMailEvent(String userId, String email, Kind kind, String code) {
    public enum Kind { REGISTER_OTP, RESET_OTP, WELCOME, PASSWORD_CHANGED, STAFF_INVITATION }
    @Override public String toString() { return "AuthMailEvent[redacted]"; }
}
