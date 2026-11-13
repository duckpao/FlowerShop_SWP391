package com.example.flowershop.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** One whole-VND calculation shared by preview, order creation and the gateway. */
public final class CustomOrderPricing {
    private CustomOrderPricing() {}
    public static BigDecimal deposit(BigDecimal subTotal, BigDecimal discount, BigDecimal total, Integer percent) {
        BigDecimal base=percent==null?total:subTotal.subtract(discount==null?BigDecimal.ZERO:discount);
        BigDecimal rate=BigDecimal.valueOf(percent==null?50:percent);
        return base.multiply(rate).divide(BigDecimal.valueOf(100),0,RoundingMode.HALF_UP)
                .max(BigDecimal.ONE).min(total.setScale(0,RoundingMode.DOWN));
    }
}
