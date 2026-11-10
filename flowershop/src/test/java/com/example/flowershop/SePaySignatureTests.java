package com.example.flowershop;

import com.example.flowershop.util.SePaySignatureUtil;
import org.junit.jupiter.api.Test;
import java.util.LinkedHashMap;
import static org.assertj.core.api.Assertions.assertThat;

class SePaySignatureTests {
    @Test void matchesIndependentHmacVectorInSePayDocumentedOrder() {
        // Insert out of order so the test does not accidentally depend on map iteration order.
        var fields=new LinkedHashMap<String,String>();
        fields.put("cancel_url","https://yoursite.com/payment/cancel");
        fields.put("order_invoice_number","INV_20231201_001");
        fields.put("merchant","MERCHANT_123");
        fields.put("order_description","Demo checkout");
        fields.put("operation","PURCHASE");
        fields.put("currency","VND");
        fields.put("order_amount","100000");
        fields.put("success_url","https://yoursite.com/payment/success");
        fields.put("error_url","https://yoursite.com/payment/error");
        // Expected digest calculated independently with Node crypto, not the production helper.
        String expected="2SLv60JXkYhk1xyKWdsp/4JC0sP6UPdnjKZyknRLvGE=";
        assertThat(SePaySignatureUtil.generateSignature(fields,"sandbox-test-secret")).isEqualTo(expected);
        fields.put("customer_id","0");
        fields.put("payment_method","");
        fields.put("lang","vi");
        fields.put("signature","ignored");
        assertThat(SePaySignatureUtil.generateSignature(fields,"sandbox-test-secret")).isEqualTo(expected);
    }
}
