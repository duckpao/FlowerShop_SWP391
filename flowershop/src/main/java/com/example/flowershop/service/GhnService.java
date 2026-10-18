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
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/**
 * Client gọi API Giao Hàng Nhanh (GHN): tra cứu tỉnh/quận/phường, tính phí ship, tạo vận đơn,
 * theo dõi, đổi trạng thái và cập nhật vận đơn.
 * (Bản gốc) Client for Giao Hàng Nhanh (GHN). Docs: https://developer.ghn.vn/en/docs/getting-started/overview
 *
 * Every call sends the {@code Token} and {@code ShopId} headers. Production is
 * https://online-gateway.ghn.vn and staging is https://dev-online-gateway.ghn.vn; the base URL is
 * {@code app.ghn.api-url}. A call succeeded only when both the HTTP status and the body's
 * {@code code} are 200.
 *
 * Addresses use the three-level model: province -> district -> ward. Master data returns
 * ProvinceID, DistrictID and WardCode; fee quotes take DistrictID plus WardCode, while order
 * creation takes the matching names with {@code is_new_*_address=false}.
 */
@Service
public class GhnService {

    /** Một mục trong danh sách địa danh GHN (id/mã phường + tên). One entry of a master-data list: the id (or ward code) to send back to GHN, and its name. */
    public record Option(String id, String name) {}

    /** Địa chỉ theo cách GHN gọi tên: đường, phường, quận, tỉnh. A postal address as GHN names it: street line, ward, district and province. */
    public record Place(String address, String ward, String district, String province) {}

    /** Kết quả tạo vận đơn: mã vận đơn (order_code) và tổng phí GHN. */
    public record CreateOrderResult(String orderCode, int totalFee) {}

    /** Trạng thái thô của GHN kèm trạng thái rút gọn lưu ở Delivery. Raw GHN status code plus the coarser status stored on our Delivery row. */
    public record Tracking(String ghnStatus, DeliveryStatus status) {}

    static final int DEFAULT_WEIGHT_GRAM = 2000;
    private static final int DEFAULT_LENGTH_CM = 30;
    private static final int DEFAULT_WIDTH_CM = 30;
    private static final int DEFAULT_HEIGHT_CM = 30;

    /** Dịch vụ GHN loại 2 (hàng dưới 20kg). service_type_id 2 is the standard service for parcels under 20 kg. */
    private static final int SERVICE_TYPE_ID = 2;

    /** Người trả phí GHN = shop, vì phí ship của khách đã nằm trong tổng đơn. payment_type_id 1: the shop pays GHN. The customer's shipping fee is already in the order total. */
    private static final int PAYMENT_TYPE_SHOP_PAYS = 1;

    private static final int WARD_PAGE_SIZE = 200;

    /** Ánh xạ mã trạng thái GHN sang DeliveryStatus của hệ thống. Order status codes from https://developer.ghn.vn/en/docs/master-data/order-status */
    private static final Map<String, DeliveryStatus> STATUS_MAP = Map.ofEntries(
            Map.entry("ready_to_pick", DeliveryStatus.PENDING),
            Map.entry("picking", DeliveryStatus.PENDING),
            Map.entry("money_collect_picking", DeliveryStatus.PENDING),
            Map.entry("picked", DeliveryStatus.PICKED_UP),
            Map.entry("storing", DeliveryStatus.PICKED_UP),
            Map.entry("sorting", DeliveryStatus.PICKED_UP),
            Map.entry("transporting", DeliveryStatus.ON_THE_WAY),
            Map.entry("delivering", DeliveryStatus.ON_THE_WAY),
            Map.entry("money_collect_delivering", DeliveryStatus.ON_THE_WAY),
            Map.entry("delivered", DeliveryStatus.DELIVERED),
            Map.entry("delivery_fail", DeliveryStatus.FAILED),
            Map.entry("waiting_to_return", DeliveryStatus.FAILED),
            Map.entry("return", DeliveryStatus.RETURNING),
            Map.entry("returning", DeliveryStatus.RETURNING),
            Map.entry("return_transporting", DeliveryStatus.RETURNING),
            Map.entry("return_sorting", DeliveryStatus.RETURNING),
            Map.entry("return_fail", DeliveryStatus.FAILED),
            Map.entry("returned", DeliveryStatus.RETURNED),
            Map.entry("cancel", DeliveryStatus.FAILED),
            Map.entry("exception", DeliveryStatus.FAILED),
            Map.entry("lost", DeliveryStatus.FAILED),
            Map.entry("damage", DeliveryStatus.FAILED),
            Map.entry("scrap", DeliveryStatus.FAILED)
    );

    /**
     * Các thao tác shop được gửi sang GHN (hủy, trả hàng, giao lại). Actions a shop may send to GHN, each POST /v2/switch-status/{action}. Pickup, transit and
     * delivery are done by GHN staff, so those statuses can only be read back with {@link #track}.
     */
    public static final Set<String> SWITCH_ACTIONS = Set.of("cancel", "return", "storing");

    /** reason_code GHN requires on cancel; GHN-CANCEL-OTHER pairs with a free-text reason. */
    private static final String CANCEL_REASON_CODE = "GHN-CANCEL-OTHER";

    /** Đổi mã trạng thái GHN sang DeliveryStatus; mã lạ coi là FAILED. */
    public static DeliveryStatus toDeliveryStatus(String ghnStatus) {
        return STATUS_MAP.getOrDefault(ghnStatus, DeliveryStatus.FAILED);
    }

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

    /** Báo 503 nếu server chưa cấu hình token/shopId GHN. */
    private void ensureConfigured() {
        if (token.isBlank() || shopId.isBlank())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Chưa cấu hình GHN trên server.");
    }

    // Master data: province -> district -> ward

    /** GET /master-data/province. ProvinceID and ProvinceName of every province. */
    public List<Option> provinces() {
        ensureConfigured();
        List<Option> list = new ArrayList<>();
        for (JsonNode n : get("/shiip/public-api/master-data/province"))
            list.add(new Option(String.valueOf(n.get("ProvinceID").asInt()), n.get("ProvinceName").asText()));
        return list;
    }

    /**
     * GET /master-data/district?province_id=. DistrictID feeds the ward lookup, fee quotes and
     * the pickup address. SupportType 0 means GHN neither picks up nor delivers there.
     */
    public List<Option> districts(int provinceId) {
        ensureConfigured();
        List<Option> list = new ArrayList<>();
        for (JsonNode n : get("/shiip/public-api/master-data/district?province_id=" + provinceId))
            list.add(new Option(String.valueOf(n.get("DistrictID").asInt()), n.get("DistrictName").asText()));
        return list;
    }

    /**
     * GET /master-data/ward?district_id=. WardCode is a string and can have leading zeros, so it
     * is never parsed to a number.
     */
    public List<Option> wards(int districtId) {
        ensureConfigured();
        List<Option> list = new ArrayList<>();
        for (JsonNode n : get("/shiip/public-api/master-data/ward?district_id=" + districtId))
            list.add(new Option(n.get("WardCode").asText(), n.get("WardName").asText()));
        return list;
    }

    // Orders

    /**
     * POST /v2/shipping-order/fee. Quotes the shipping fee before the customer orders; the quote
     * is the "total" field. Only weight is mandatory. The pickup side is the shop's own district
     * and ward, so the quote does not depend on the pickup address registered on GHN.
     */
    public BigDecimal calculateFee(int fromDistrictId, String fromWardCode, int toDistrictId, String toWardCode, int weightGram) {
        ensureConfigured();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("from_district_id", fromDistrictId);
        body.put("from_ward_code", fromWardCode);
        body.put("to_district_id", toDistrictId);
        body.put("to_ward_code", toWardCode);
        body.put("service_type_id", SERVICE_TYPE_ID);
        body.put("weight", weightGram);
        body.put("length", DEFAULT_LENGTH_CM);
        body.put("width", DEFAULT_WIDTH_CM);
        body.put("height", DEFAULT_HEIGHT_CM);
        return BigDecimal.valueOf(post("/shiip/public-api/v2/shipping-order/fee", body).get("total").asInt());
    }

    /**
     * POST /v2/shipping-order/create. Creates the waybill and returns order_code, the key for
     * every later call.
     *
     * Addresses go by name; is_new_*_address=false is the three-level model and needs the district.
     * from_* overrides the pickup address registered on the GHN shop, and return_* sends refused
     * parcels back to the same address, so one GHN shop can serve many branches.
     * cod_amount is 0 for orders already paid online; payment_type_id 1 makes the shop pay GHN
     * because the customer's fee is already in the order total.
     */
    public CreateOrderResult createOrder(String toName, String toPhone, Place to,
                                         String fromName, String fromPhone, Place from,
                                         int codAmount, String productName, int quantity, int price) {
        ensureConfigured();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("to_name", toName);
        body.put("to_phone", toPhone);
        body.put("to_address", to.address());
        body.put("to_ward_name", to.ward());
        body.put("to_district_name", to.district());
        body.put("to_province_name", to.province());
        body.put("is_new_to_address", false);
        body.put("from_name", fromName);
        body.put("from_phone", fromPhone);
        body.put("from_address", from.address());
        body.put("from_ward_name", from.ward());
        body.put("from_district_name", from.district());
        body.put("from_province_name", from.province());
        body.put("is_new_from_address", false);
        body.put("return_name", fromName);
        body.put("return_phone", fromPhone);
        body.put("return_address", from.address());
        body.put("return_ward_name", from.ward());
        body.put("return_district_name", from.district());
        body.put("return_province_name", from.province());
        body.put("is_new_return_address", false);
        body.put("weight", DEFAULT_WEIGHT_GRAM);
        body.put("length", DEFAULT_LENGTH_CM);
        body.put("width", DEFAULT_WIDTH_CM);
        body.put("height", DEFAULT_HEIGHT_CM);
        body.put("service_type_id", SERVICE_TYPE_ID);
        body.put("payment_type_id", PAYMENT_TYPE_SHOP_PAYS);
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
        return new CreateOrderResult(data.get("order_code").asText(), data.get("total_fee").asInt());
    }

    /**
     * GET /v2/shipping-order/detail?order_code=. Reads the live status of one waybill; the
     * order must belong to a shop of the token's account. {@code data.status} is one of the
     * order status codes.
     */
    public Tracking track(String orderCode) {
        ensureConfigured();
        JsonNode data = get("/shiip/public-api/v2/shipping-order/detail?order_code="
                + URLEncoder.encode(orderCode, StandardCharsets.UTF_8));
        String status = data.path("status").asText();
        return new Tracking(status, toDeliveryStatus(status));
    }

    /**
     * POST /v2/switch-status/{cancel|return|storing}. Shop-side status changes:
     * cancel works while GHN has not picked the parcel up; return needs delivery_fail, storing,
     * waiting_to_return or return; storing (redelivery) needs storing or waiting_to_return.
     *
     * Cancel is all-or-nothing and answers 400 for an unknown code, while return and storing
     * answer 200 with a per-order {@code result}. Both are checked, because a 200 can still be a refusal.
     */
    public void switchStatus(String orderCode, String action) {
        if (!SWITCH_ACTIONS.contains(action))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "GHN không hỗ trợ thao tác: " + action);
        ensureConfigured();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("order_codes", List.of(orderCode));
        if (action.equals("cancel")) {
            body.put("reason_code", CANCEL_REASON_CODE);
            body.put("reason", "Cửa hàng hủy vận đơn");
        }
        for (JsonNode r : post("/shiip/public-api/v2/switch-status/" + action, body)) {
            if (!r.path("result").asBoolean(false))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "GHN từ chối thao tác: " + r.path("message").asText("không rõ lý do"));
        }
    }

    /**
     * POST /v2/shipping-order/update. Partial update: send order_code plus only the Create Order
     * fields to change. Which fields are editable depends on the status (everything at
     * ready_to_pick, receiver info until delivered, nothing after delivered or cancel).
     * Changing cod_amount additionally needs an OTP GHN sends to the shop's phone.
     */
    public void updateOrder(String orderCode, Map<String, Object> updateFields) {
        ensureConfigured();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("order_code", orderCode);
        body.putAll(updateFields);
        post("/shiip/public-api/v2/shipping-order/update", body);
    }

    // HTTP

    /** Gọi GET tới GHN, trả về phần data. */
    private JsonNode get(String path) {
        return send(base(path).GET().build(), path);
    }

    /** Gọi POST JSON tới GHN, trả về phần data. */
    private JsonNode post(String path, Map<String, Object> body) {
        try {
            HttpRequest request = base(path)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();
            return send(request, path);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Không thể kết nối tới GHN.", e);
        }
    }

    /** Tạo request có sẵn header Token và ShopId. */
    private HttpRequest.Builder base(String path) {
        return HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + path))
                .timeout(Duration.ofSeconds(30))
                .header("Token", token)
                .header("ShopId", shopId);
    }

    /** Sends the request and returns the body's {@code data}; anything but 200/200 becomes a 502. */
    private JsonNode send(HttpRequest request, String path) {
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = mapper.readTree(response.body());
            if (response.statusCode() != 200 || json.path("code").asInt(-1) != 200)
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, failureMessage(json, path));
            return json.path("data");
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Không thể kết nối tới GHN.", e);
        }
    }

    /** Đổi lỗi GHN thành thông báo tiếng Việt dễ hiểu. */
    private static String failureMessage(JsonNode json, String path) {
        String msg = json.path("message").asText("GHN " + path + " thất bại.");
        String codeMessage = json.path("code_message").asText("");
        if (msg.contains("inside_warehouse_route_by_address") || msg.contains("Connection not open")
                || codeMessage.contains("WAREHOUSE_NOT_FOUND"))
            return "GHN đang lỗi định tuyến kho (inside_warehouse_route_by_address). Lỗi từ phía GHN, hãy thử lại sau ít phút.";
        if (codeMessage.contains("CLIENT_NOT_BELONG_OF_SHOP") || codeMessage.contains("CLIENT_NOT_OWNER_OF_SHOP"))
            return "Token GHN không thuộc ShopId đã cấu hình. Kiểm tra GHN_TOKEN và GHN_SHOP_ID.";
        return msg;
    }
}
