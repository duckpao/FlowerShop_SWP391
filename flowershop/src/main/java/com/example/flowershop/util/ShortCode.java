package com.example.flowershop.util;

public final class ShortCode {
    private ShortCode() {}

    /** Mã ngắn tối đa 8 ký tự; id ngắn hơn (dữ liệu mẫu) giữ nguyên thay vì ném lỗi. */
    public static String of(String id) {
        String clean = id.replace("-", "");
        return (clean.length() > 8 ? clean.substring(0, 8) : clean).toUpperCase();
    }
}
