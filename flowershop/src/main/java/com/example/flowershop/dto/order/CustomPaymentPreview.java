package com.example.flowershop.dto.order;

import com.example.flowershop.service.CouponService;
import com.example.flowershop.service.CustomOrderPricing;
import java.math.BigDecimal;
import java.util.List;

/** Voucher eligibility and the exact deposit/balance returned together for one checkout. */
public record CustomPaymentPreview(List<CouponService.Choice> orderVouchers,
        List<CouponService.Choice> shippingVouchers, String recommendedOrderCode,
        String recommendedShippingCode, BigDecimal bestSaving, BigDecimal discountAmount,
        BigDecimal shippingDiscountAmount, BigDecimal totalAmount, BigDecimal subTotal,
        BigDecimal shippingFee, BigDecimal depositAmount, BigDecimal remainingAmount, Integer depositPercent) {
    public static CustomPaymentPreview from(CouponService.Preview preview, BigDecimal subTotal,
            BigDecimal shippingFee, Integer percent) {
        BigDecimal deposit=CustomOrderPricing.deposit(subTotal,preview.discountAmount(),preview.totalAmount(),percent);
        return new CustomPaymentPreview(preview.orderVouchers(),preview.shippingVouchers(),
                preview.recommendedOrderCode(),preview.recommendedShippingCode(),preview.bestSaving(),
                preview.discountAmount(),preview.shippingDiscountAmount(),preview.totalAmount(),subTotal,
                shippingFee,deposit,preview.totalAmount().subtract(deposit),percent);
    }
}
