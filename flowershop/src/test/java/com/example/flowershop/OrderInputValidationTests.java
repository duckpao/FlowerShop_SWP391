package com.example.flowershop;

import com.example.flowershop.dto.order.MakeCustomOrderRequest;
import com.example.flowershop.dto.order.MakeOrderRequest;
import com.example.flowershop.dto.order.MakeOrdersRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Tên và số điện thoại người nhận được kiểm ngay ở request nên lỗi trả 400, không chạm tới DB. */
class OrderInputValidationTests {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private MakeOrdersRequest batch(String name, String phone) {
        var shop = new MakeOrdersRequest.ShopOrder();
        shop.setShopId("s-1");
        shop.setCartItemIds(List.of("ci-1"));
        var request = new MakeOrdersRequest();
        request.setDeliveryAddressId("a-1");
        request.setShops(List.of(shop));
        request.setRecipientName(name);
        request.setPhone(phone);
        return request;
    }

    @Test
    void validRecipientPassesAndBlankFallsBackToProfile() {
        assertThat(validator.validate(batch("Lan", "0901234567"))).isEmpty();
        assertThat(validator.validate(batch(null, null))).isEmpty();
        assertThat(validator.validate(batch("Lan", ""))).isEmpty();
    }

    @Test
    void nonNumericOrWrongLengthPhoneIsRejected() {
        for (String phone : List.of("abc", "12345", "1901234567", "09012345678", "090123456a"))
            assertThat(validator.validate(batch("Lan", phone))).as(phone).isNotEmpty();
    }

    @Test
    void recipientNameOverOneHundredCharactersIsRejected() {
        assertThat(validator.validate(batch("a".repeat(100), "0901234567"))).isEmpty();
        assertThat(validator.validate(batch("a".repeat(101), "0901234567"))).isNotEmpty();
    }

    @Test
    void singleAndCustomOrderRequestsApplyTheSameRules() {
        var single = new MakeOrderRequest();
        single.setShopId("s-1");
        single.setCartItemIds(List.of("ci-1"));
        single.setDeliveryAddressId("a-1");
        single.setPhone("abc");
        single.setRecipientName("a".repeat(101));
        assertThat(validator.validate(single)).hasSize(2);

        var custom = new MakeCustomOrderRequest();
        custom.setDeliveryAddressId("a-1");
        custom.setPhone("abc");
        custom.setRecipientName("a".repeat(101));
        assertThat(validator.validate(custom)).hasSize(2);
    }
}
