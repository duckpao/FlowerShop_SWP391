package com.example.flowershop.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class SePaySignatureUtil {

    private static final List<String> SIGNED_FIELD_ORDER = List.of(
            "merchant",
            "operation",
            "payment_method",
            "order_amount",
            "currency",
            "order_invoice_number",
            "order_description",
            "customer_id",
            "success_url",
            "error_url",
            "cancel_url"
    );

    /**
     * Tạo chữ ký HMAC-SHA256 (Base64) theo đúng chuẩn tài liệu SePay Gateway:
     * field1=value1,field2=value2,field3=value3...
     */
    public static String generateSignature(Map<String, String> fields, String secretKey) {
        try {
            List<String> signedPairs = new ArrayList<>();
            for (String fieldName : SIGNED_FIELD_ORDER) {
                String value = fields.get(fieldName);
                if (value != null && !value.isBlank()) {
                    signedPairs.add(fieldName + "=" + value);
                }
            }

            String signedString = String.join(",", signedPairs);

            Mac hmacSha256 = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmacSha256.init(keySpec);

            byte[] hash = hmacSha256.doFinal(signedString.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi tạo chữ ký SePay signature: " + e.getMessage(), e);
        }
    }
}

