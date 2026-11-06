package com.example.flowershop.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class GhnServiceTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private HttpServer server;
    private final AtomicReference<String> lastPath = new AtomicReference<>();
    private final AtomicReference<String> lastBody = new AtomicReference<>();
    private final AtomicReference<String> lastShopId = new AtomicReference<>();
    private volatile String reply;

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            lastPath.set(exchange.getRequestURI().toString());
            lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            lastShopId.set(exchange.getRequestHeaders().getFirst("ShopId"));
            byte[] out = reply.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, out.length);
            exchange.getResponseBody().write(out);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stop() { server.stop(0); }

    private GhnService ghn() {
        return new GhnService("http://127.0.0.1:" + server.getAddress().getPort(), "tok", "226554");
    }

    private JsonNode sentBody() throws Exception { return mapper.readTree(lastBody.get()); }

    @Test
    void createOrderSendsPickupAndReturnAddressOfTheBranch() throws Exception {
        reply = "{\"code\":200,\"data\":{\"order_code\":\"LAD1\",\"total_fee\":22000}}";
        var to = new GhnService.Place("10 Cầu Giấy", "Dịch Vọng", "Cầu Giấy", "Hà Nội");
        var from = new GhnService.Place("Khu CNC", "Thạch Hòa", "Thạch Thất", "Hà Nội");

        var result = ghn().createOrder("An", "0987654321", to, "Shop A", "0912345678", from, 0, "Hoa", 1, 350000);

        assertEquals("LAD1", result.orderCode());
        JsonNode b = sentBody();
        assertEquals("Thạch Hòa", b.get("from_ward_name").asText());
        assertEquals("Thạch Thất", b.get("from_district_name").asText());
        assertEquals("Hà Nội", b.get("from_province_name").asText());
        assertFalse(b.get("is_new_from_address").asBoolean());
        assertEquals("Khu CNC", b.get("return_address").asText());
        assertEquals("Thạch Thất", b.get("return_district_name").asText());
        assertEquals("0912345678", b.get("return_phone").asText());
        assertEquals("Cầu Giấy", b.get("to_district_name").asText());
        assertFalse(b.get("is_new_to_address").asBoolean());
        assertEquals("226554", lastShopId.get());
    }

    @Test
    void calculateFeeSendsBothEndsAsDistrictAndWardCode() throws Exception {
        reply = "{\"code\":200,\"data\":{\"total\":31000}}";

        var fee = ghn().calculateFee(3440, "13010", 1442, "20101", 2000);

        assertEquals(31000, fee.intValue());
        JsonNode b = sentBody();
        assertEquals(3440, b.get("from_district_id").asInt());
        assertEquals("13010", b.get("from_ward_code").asText());
        assertEquals(1442, b.get("to_district_id").asInt());
        assertEquals("20101", b.get("to_ward_code").asText());
    }

    @Test
    void trackReadsStatusWithGetAndMapsIt() {
        reply = "{\"code\":200,\"data\":{\"status\":\"delivering\"}}";

        var tracking = ghn().track("LAD1");

        assertEquals("delivering", tracking.ghnStatus());
        assertEquals("ON_THE_WAY", tracking.status().name());
        assertEquals("/shiip/public-api/v2/shipping-order/detail?order_code=LAD1", lastPath.get());
        assertTrue(lastBody.get().isEmpty());
    }

    @Test
    void cancelSendsReasonCodeAndRejectsPerOrderRefusal() throws Exception {
        reply = "{\"code\":200,\"data\":[{\"order_code\":\"LAD1\",\"result\":true,\"message\":\"OK\"}]}";
        ghn().switchStatus("LAD1", "cancel");
        assertEquals("GHN-CANCEL-OTHER", sentBody().get("reason_code").asText());

        reply = "{\"code\":200,\"data\":[{\"order_code\":\"LAD1\",\"result\":false,\"message\":\"Trạng thái không hợp lệ\"}]}";
        var e = assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> ghn().switchStatus("LAD1", "cancel"));
        assertTrue(e.getReason().contains("Trạng thái không hợp lệ"));
    }

    @Test
    void unsupportedSwitchActionIsRejectedBeforeAnyRequest() {
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> ghn().switchStatus("LAD1", "delivered"));
        assertNull(lastPath.get());
    }

    @Test
    void ghnErrorBodySurfacesAsBadGateway() {
        reply = "{\"code\":400,\"message\":\"Lỗi hệ thống - không lấy được thông tin kho\",\"data\":null}";
        var e = assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> ghn().calculateFee(1, "1", 2, "2", 2000));
        assertEquals(502, e.getStatusCode().value());
        assertTrue(e.getReason().contains("không lấy được thông tin kho"));
    }
}
