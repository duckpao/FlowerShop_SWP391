package com.example.flowershop;

import com.example.flowershop.controller.PaymentReturnController;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PaymentReturnTests {
    @Test void oldGatewayLinksReturnToFrontendWithInvoiceAndQueryIntact() throws Exception {
        var controller = new PaymentReturnController();
        ReflectionTestUtils.setField(controller, "frontendBaseUrl", "http://localhost:5173/");
        var mvc = MockMvcBuilders.standaloneSetup(controller).build();
        for (String result : new String[]{"success", "error", "cancel"}) {
            mvc.perform(get("http://localhost:8080/payment/" + result + "?invoice_number=INV-DEPOSIT-123&orderId=order-1"))
                    .andExpect(status().isFound())
                    .andExpect(redirectedUrl("http://localhost:5173/payment/" + result + "?invoice_number=INV-DEPOSIT-123&orderId=order-1"));
        }
    }

    @Test void sameOriginDeploymentForwardsToReactWithoutRedirectLoop() throws Exception {
        var controller = new PaymentReturnController();
        ReflectionTestUtils.setField(controller, "frontendBaseUrl", "http://localhost:8080");
        MockMvcBuilders.standaloneSetup(controller).build().perform(get("http://localhost:8080/payment/success?invoice_number=INV-1"))
                .andExpect(status().isOk()).andExpect(forwardedUrl("/index.html"));
    }
}
