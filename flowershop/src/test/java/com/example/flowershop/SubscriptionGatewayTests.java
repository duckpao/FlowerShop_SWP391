package com.example.flowershop;
import com.example.flowershop.repository.*;
import com.example.flowershop.service.*;
import com.example.flowershop.util.SePaySignatureUtil;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SubscriptionGatewayTests {
    SePayGatewayService gateway;
    HttpServer server;
    String response;
    @BeforeEach void setup() throws Exception {
        gateway=PaymentBusinessTests.newGateway(mock(OrderRepository.class),mock(PaymentRepository.class),mock(RefundRepository.class),mock(OrderService.class));
        ReflectionTestUtils.setField(gateway,"merchantId","merchant-test");
        ReflectionTestUtils.setField(gateway,"secretKey","secret-test");
        ReflectionTestUtils.setField(gateway,"checkoutUrl","https://gateway.example.test");
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/v1/order/detail/SUB-test",exchange->{
            byte[] body=response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type","application/json");
            exchange.sendResponseHeaders(200,body.length);exchange.getResponseBody().write(body);exchange.close();
        });server.start();
        ReflectionTestUtils.setField(gateway,"apiBaseUrl","http://127.0.0.1:"+server.getAddress().getPort());
    }
    @AfterEach void stop(){server.stop(0);}
    String captured(String invoice,String currency,String status){return "{\"data\":{\"order_status\":\""+status+"\",\"order_invoice_number\":\""+invoice+"\",\"order_amount\":\"59000.00\",\"order_currency\":\""+currency+"\",\"order_id\":\"SEPAY-1\"}}";}
    @Test void onlyMatchingCapturedVndInvoiceIsAccepted() {
        response=captured("SUB-test","VND","CAPTURED");
        assertThat(gateway.capturedSubscriptionInvoice("SUB-test")).hasValueSatisfying(v->assertThat(v.amount()).isEqualByComparingTo("59000"));
        for(String body:new String[]{captured("OTHER","VND","CAPTURED"),captured("SUB-test","USD","CAPTURED"),captured("SUB-test","VND","AUTHENTICATION_NOT_NEEDED"),"{\"data\":{\"order_status\":\"CAPTURED\"}}","invalid"}) {
            response=body;assertThat(gateway.capturedSubscriptionInvoice("SUB-test")).isEmpty();
        }
    }
    @Test void checkoutSignatureCoversSubscriptionReturnUrlsAndServerAmount() {
        ReflectionTestUtils.setField(gateway,"successUrl","http://localhost/payment/success");
        ReflectionTestUtils.setField(gateway,"errorUrl","http://localhost/payment/error");
        ReflectionTestUtils.setField(gateway,"cancelUrl","http://localhost/payment/cancel");
        var checkout=gateway.subscriptionCheckout("SUB-test",new BigDecimal("59000"),"owner","http://localhost/shop-admin/subscription");
        assertThat(checkout.fields()).containsEntry("order_amount","59000").containsEntry("currency","VND");
        assertThat(checkout.fields().get("success_url")).startsWith("http://localhost/shop-admin/subscription?");
        assertThat(checkout.fields()).containsEntry("success_url","http://localhost/shop-admin/subscription?invoice_number=SUB-test&result=success")
                .containsEntry("error_url","http://localhost/shop-admin/subscription?invoice_number=SUB-test&result=error")
                .containsEntry("cancel_url","http://localhost/shop-admin/subscription?invoice_number=SUB-test&result=cancel");
        assertThat(checkout.fields().get("signature")).isEqualTo(SePaySignatureUtil.generateSignature(checkout.fields(),"secret-test"));
    }
    @Test void sandboxCredentialsAreTrimmedAndFormUsesDocumentedFieldOrder() {
        ReflectionTestUtils.setField(gateway,"merchantId"," SP-TEST-demo ");
        ReflectionTestUtils.setField(gateway,"secretKey"," secret-test ");
        ReflectionTestUtils.setField(gateway,"checkoutUrl"," https://pay-sandbox.sepay.vn/v1/checkout/init ");
        ReflectionTestUtils.invokeMethod(gateway,"normalizeConfiguration");
        var checkout=gateway.subscriptionCheckout("SUB-test",new BigDecimal("59000"),"owner","http://localhost/shop-admin/subscription");
        assertThat(checkout.checkoutUrl()).isEqualTo("https://pay-sandbox.sepay.vn/v1/checkout/init");
        assertThat(checkout.fields()).containsEntry("merchant","SP-TEST-demo");
        assertThat(checkout.fields().get("signature")).isEqualTo(SePaySignatureUtil.generateSignature(checkout.fields(),"secret-test"));
        assertThat(checkout.fields().keySet()).containsExactly("order_amount","merchant","currency","operation",
                "order_description","order_invoice_number","customer_id","success_url","error_url","cancel_url","signature");
    }
}
