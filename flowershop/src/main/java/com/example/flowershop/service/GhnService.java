package com.example.flowershop.service;

import com.example.flowershop.entity.enums.DeliveryStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@Service
public class GhnService {

    public record Option(String id, String name) {}
    public record CreateOrderResult(String orderCode, int totalFee) {}

    static final int DEFAULT_WEIGHT_GRAM = 2000;
    private static final int DEFAULT_LENGTH_CM = 30;
    private static final int DEFAULT_WIDTH_CM = 30;
    private static final int DEFAULT_HEIGHT_CM = 30;
    private static final int SERVICE_TYPE_ID = 2;

    private static final Map<String, DeliveryStatus> STATUS_MAP = Map.ofEntries(
            Map.entry("ready_to_pick", DeliveryStatus.PENDING),
            Map.entry("picking", DeliveryStatus.PENDING),
            Map.entry("storing", DeliveryStatus.PENDING),
            Map.entry("picked", DeliveryStatus.PICKED_UP),
            Map.entry("sorting", DeliveryStatus.PICKED_UP),
            Map.entry("transporting", DeliveryStatus.ON_THE_WAY),
            Map.entry("delivering", DeliveryStatus.ON_THE_WAY),
            Map.entry("delivered", DeliveryStatus.DELIVERED)
    );

    private final String apiUrl;
    private final String token;
    private final String shopId;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    public GhnService(@Value("${app.ghn.api-url}") String apiUrl,
                      @Value("${app.ghn.token}") String token,
                      @Value("${app.ghn.shop-id}") String shopId) {
        this.apiUrl = apiUrl;
        this.token = token;
        this.shopId = shopId;
    }

    private void ensureConfigured() {
        if (token.isBlank() || shopId.isBlank())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Chưa cấu hình GHN trên server.");
    }

    public List<Option> provinces() {
        ensureConfigured();
        JsonNode data = get("/shiip/public-api/master-data/province");
        List<Option> list = new ArrayList<>();
        for (JsonNode n : data) {
            int id = n.get("ProvinceID").asInt();
            String name = n.get("ProvinceName").asText();
            list.add(new Option(String.valueOf(id), name));
        }
        return list;
    }

    public List<Option> districts(int provinceId) {
        ensureConfigured();
        JsonNode data = get("/shiip/public-api/master-data/district?province_id=" + provinceId);
        List<Option> list = new ArrayList<>();
        for (JsonNode n : data) {
            int id = n.get("DistrictID").asInt();
            String name = n.get("DistrictName").asText();
            list.add(new Option(String.valueOf(id), name));
        }
        return list;
    }

    public List<Option> wards(int districtId) {
        ensureConfigured();
        JsonNode data = get("/shiip/public-api/master-data/ward?district_id=" + districtId);
        List<Option> list = new ArrayList<>();
        for (JsonNode n : data) {
            String code = n.get("WardCode").asText();
            String name = n.get("WardName").asText();
            list.add(new Option(code, name));
        }
        return list;
    }

    public BigDecimal calculateFee(int fromDistrictId, int toDistrictId, String toWardCode, int weightGram) {
        ensureConfigured();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("from_district_id", fromDistrictId);
        body.put("to_district_id", toDistrictId);
        body.put("to_ward_code", toWardCode);
        body.put("service_type_id", SERVICE_TYPE_ID);
        body.put("weight", weightGram);
        body.put("length", DEFAULT_LENGTH_CM);
        body.put("width", DEFAULT_WIDTH_CM);
        body.put("height", DEFAULT_HEIGHT_CM);
        JsonNode data = post("/shiip/public-api/v2/shipping-order/fee", body);
        return BigDecimal.valueOf(data.get("total").asInt());
    }

    public CreateOrderResult createOrder(String toName, String toPhone, String toAddress,
                                          int toDistrictId, String toWardCode,
                                          String fromName, String fromPhone, String fromAddress,
                                          int fromDistrictId, String fromWardCode,
                                          int codAmount, String productName, int quantity, int price) {
        ensureConfigured();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("to_name", toName);
        body.put("to_phone", toPhone != null ? toPhone : "");
        body.put("to_address", toAddress);
        body.put("to_ward_code", toWardCode);
        body.put("to_district_id", toDistrictId);
        body.put("from_name", fromName);
        body.put("from_phone", fromPhone != null ? fromPhone : "");
        body.put("from_address", fromAddress);
        body.put("from_ward_code", fromWardCode);
        body.put("from_district_id", fromDistrictId);
        body.put("weight", DEFAULT_WEIGHT_GRAM);
        body.put("length", DEFAULT_LENGTH_CM);
        body.put("width", DEFAULT_WIDTH_CM);
        body.put("height", DEFAULT_HEIGHT_CM);
        body.put("service_type_id", SERVICE_TYPE_ID);
        body.put("payment_type_id", 2);
        body.put("required_note", "CHOXEMHANGKHONGTHU");
        body.put("cod_amount", codAmount);
        body.put("content", "Đơn hàng hoa - FlowerShop");
        body.put("items", List.of(Map.of(
                "name", productName != null ? productName : "Hoa tươi",
                "code", "FLOWER-01",
                "quantity", quantity,
                "price", price,
                "weight", DEFAULT_WEIGHT_GRAM
        )));
        JsonNode data = post("/shiip/public-api/v2/shipping-order/create", body);
        String orderCode = data.get("order_code").asText();
        int totalFee = data.get("total_fee").asInt();
        return new CreateOrderResult(orderCode, totalFee);
    }
// --- HTTP helpers ---

    private JsonNode get(String path) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl + path))
                    .timeout(Duration.ofSeconds(30))
                    .header("Token", token)
                    .header("ShopId", shopId)
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = mapper.readTree(response.body());
            int code = json.path("code").asInt(-1);
            if (response.statusCode() != 200 || code != 200)
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        json.path("message").asText("GHN " + path + " thất bại."));
            return json.path("data");
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Không thể kết nối tới GHN.", e);
        }
    }

    private JsonNode post(String path, Map<String, Object> body) {
        try {
            String jsonBody = mapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl + path))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .header("Token", token)
                    .header("ShopId", shopId)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = mapper.readTree(response.body());
            int code = json.path("code").asInt(-1);
            if (response.statusCode() != 200 || code != 200)
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        json.path("message").asText("GHN " + path + " thất bại."));
            return json.path("data");
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Không thể kết nối tới GHN.", e);
        }
    }

    public DeliveryStatus checkStatus(String orderCode) {
        ensureConfigured();
        JsonNode data = get("/shiip/public-api/v2/shipping-order/detail?order_code=" + orderCode);
        String status = data.path("status").asText();
        return STATUS_MAP.getOrDefault(status, DeliveryStatus.FAILED);
    }
}