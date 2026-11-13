package com.example.flowershop;
import com.example.flowershop.service.CustomOrderPricing;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
class CustomOrderPricingTests {
    @Test void discountCannotRoundPercentageDepositToZeroOrTriggerLegacyFallback() {
        assertThat(CustomOrderPricing.deposit(new BigDecimal("100"),new BigDecimal("99"),new BigDecimal("30001"),10))
                .isEqualByComparingTo("1");
    }
    @Test void legacyHalfDepositIsWholeDongAndBalanceNeverNegative() {
        assertThat(CustomOrderPricing.deposit(new BigDecimal("101"),BigDecimal.ZERO,new BigDecimal("101"),null))
                .isEqualByComparingTo("51");
        assertThat(CustomOrderPricing.deposit(BigDecimal.ONE,BigDecimal.ZERO,BigDecimal.ONE,100))
                .isEqualByComparingTo("1");
    }
}
