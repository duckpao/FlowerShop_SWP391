package com.example.flowershop.service;

import com.example.flowershop.entity.*;
import com.example.flowershop.entity.enums.*;
import com.example.flowershop.repository.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;

/**
 * Service xử lý nghiệp vụ quản lý đơn hàng dành cho Chủ shop (Shop Manager).
 * Bao gồm các luồng chính:
 * 1. Xem danh sách đơn hàng và hàng đợi (Order Queue).
 * 2. Xác nhận đơn hàng (PENDING -> PROCESSING).
 * 3. Tạo mã vận đơn GHN và chuyển trạng thái giao hàng (PROCESSING -> DELIVERING).
 * 4. Đồng bộ / Giả lập trạng thái giao hàng GHN (DELIVERING -> COMPLETED).
 * 5. Hủy đơn hàng trước khi giao.
 */
@Service
@Transactional(readOnly = true)
public class ManagerOrderService {

    // --- CÁC DTO RECORDS PHỤC VỤ TRẢ VỀ DỮ LIỆU ---

    /** Tóm tắt đơn hàng hiển thị trong danh sách bảng */
    public record OrderSummary(String id, String customerName, String customerPhone,
                               BigDecimal totalAmount, String status, java.time.LocalDateTime createdDate) {}

    /** Chi tiết đầy đủ của đơn hàng hiển thị trong modal xem chi tiết */
    public record OrderDetailResponse(String id, String customerId, String customerName, String customerPhone,
                                      String recipientName, String recipientPhone, String deliveryAddress, 
                                      String shopAddress, String status, BigDecimal subTotal,
                                      BigDecimal shippingFee, BigDecimal totalAmount,
                                      java.time.LocalDateTime createdDate, String note,
                                      List<OrderItemResponse> items, DeliveryInfo delivery, PaymentInfo payment) {}

    /** Thông tin từng món hàng trong đơn */
    public record OrderItemResponse(String productId, String productName, BigDecimal price, Integer quantity) {}

    /** Thông tin vận đơn đối tác giao hàng (GHN) */
    public record DeliveryInfo(String trackingCode, String deliveryPartnerId, String status) {}

    /** Thông tin thanh toán (phương thức, trạng thái, số tiền) */
    public record PaymentInfo(String id, String method, String status, BigDecimal amount) {}

    /** Kết quả phân trang danh sách đơn hàng */
    public record Results(List<OrderSummary> content, int page, int totalPages, long totalElements) {}

    /** Kết quả trả về sau khi tạo vận đơn GHN thành công */
    public record ShipResult(String trackingCode) {}

    private final OrderRepository orders;
    private final OrderDetailRepository orderDetails;
    private final DeliveryRepository deliveries;
    private final PaymentRepository payments;
    private final ShopRepository shops;
    private final AddressRepository addresses;
    private final GhnService ghn;

    public ManagerOrderService(OrderRepository orders, OrderDetailRepository orderDetails,
                               DeliveryRepository deliveries, PaymentRepository payments,
                               ShopRepository shops, AddressRepository addresses, GhnService ghn) {
        this.orders = orders;
        this.orderDetails = orderDetails;
        this.deliveries = deliveries;
        this.payments = payments;
        this.shops = shops;
        this.addresses = addresses;
        this.ghn = ghn;
    }

    /** Tạo ngoại lệ 404 khi không tìm thấy đơn hàng */
    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng.");
    }

    /**
     * Kiểm tra quyền sở hữu của người thao tác (actor) đối với shopId.
     * Đảm bảo người dùng chỉ có thể quản lý đúng shop thuộc quyền của mình.
     */
    private Shop owned(String shopId, String actor) {
        Shop s = shops.findById(shopId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy shop."));
        if (!s.getOwner().getId().equals(actor))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không sở hữu shop này.");
        return s;
    }

    /**
     * Lấy danh sách tất cả đơn hàng của shop theo phân trang và lọc theo trạng thái nếu có.
     */
    public Results list(String shopId, String actor, int page, String status) {
        owned(shopId, actor);
        Pageable pageable = PageRequest.of(page, 20, Sort.by(Sort.Order.desc("createdDate"), Sort.Order.asc("id")));
        Page<Order> result;
        if (status != null && !status.isBlank()) {
            try {
                OrderStatus st = OrderStatus.valueOf(status.toUpperCase());
                result = orders.findByShopIdAndStatus(shopId, st, pageable);
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trạng thái không hợp lệ: " + status);
            }
        } else {
            result = orders.findByShopId(shopId, pageable);
        }
        var content = result.map(o -> new OrderSummary(o.getId(),
                o.getCustomer().getFullName(), o.getCustomer().getPhone(),
                o.getTotalAmount(), o.getStatus().name(), o.getCreatedDate())).toList();
        return new Results(content, page, result.getTotalPages(), result.getTotalElements());
    }

    /**
     * Xem thông tin chi tiết một đơn hàng của shop (kèm sản phẩm, địa chỉ, vận đơn, thanh toán).
     */
    public OrderDetailResponse detail(String shopId, String actor, String orderId) {
        owned(shopId, actor);
        Order o = orders.findByIdAndShopId(orderId, shopId).orElseThrow(ManagerOrderService::notFound);
        return buildDetail(o);
    }

    /**
     * Xác nhận đơn hàng: Chuyển trạng thái từ PENDING (Chờ xác nhận) sang PROCESSING (Đang chuẩn bị).
     * Lúc này shop bắt đầu làm hoa, chưa gọi API vận chuyển GHN.
     */
    @Transactional
    public void confirm(String shopId, String actor, String orderId) {
        owned(shopId, actor);
        Order o = orders.findByIdAndShopId(orderId, shopId).orElseThrow(ManagerOrderService::notFound);
        if (o.getStatus() != OrderStatus.PENDING)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ đơn hàng PENDING mới có thể xác nhận.");
        o.setStatus(OrderStatus.PROCESSING);
        o.setLastModifyBy(actor);
    }

    /**
     * Tạo vận đơn GHN và chuyển đơn sang DELIVERING (Đang giao).
     * Quy trình:
     * 1. Xác thực đơn hàng đang ở trạng thái PROCESSING.
     * 2. Lấy địa chỉ lấy hàng mặc định của Shop (tự động phân giải mã GHN nếu là dữ liệu cũ).
     * 3. Lấy địa chỉ nhận hàng của khách (tự động phân giải mã GHN nếu chưa có).
     * 4. Gọi API GHN /shipping-order/create với đầy đủ thông tin người gửi, người nhận, kích thước và hàng hóa.
     * 5. Lưu bản ghi Delivery với tracking code nhận được từ GHN.
     * 6. Cập nhật Order sang DELIVERING.
     */
    @Transactional
    public ShipResult ship(String shopId, String actor, String orderId) {
        owned(shopId, actor);
        Order o = orders.findByIdAndShopId(orderId, shopId).orElseThrow(ManagerOrderService::notFound);
        if (o.getStatus() != OrderStatus.PROCESSING)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ đơn hàng PROCESSING mới có thể gửi GHN.");

        // Lấy địa chỉ kho/shop gửi hàng
        var shopAddrs = addresses.findByShopIdAndUserIsNullOrderByCreatedDateAscIdAsc(shopId);
        Address shopAddr = shopAddrs.stream().filter(a -> Boolean.TRUE.equals(a.getIsDefault())).findFirst()
                .orElse(shopAddrs.isEmpty() ? null : shopAddrs.get(0));
        if (shopAddr == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Shop chưa cấu hình địa chỉ.");
        }
        if (shopAddr.getGhnDistrictId() == null || shopAddr.getGhnWardCode() == null) {
            resolveGhnAddress(shopAddr);
            addresses.save(shopAddr);
        }

        // Lấy địa chỉ người nhận
        Address toAddr = o.getDeliveryAddress();
        if (toAddr == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn hàng chưa có địa chỉ nhận.");
        }
        if (toAddr.getGhnDistrictId() == null || toAddr.getGhnWardCode() == null) {
            resolveGhnAddress(toAddr);
            addresses.save(toAddr);
        }

        var details = orderDetails.findByOrderId(orderId);
        String productName = "Hoa tươi"; int quantity = 1, price = o.getTotalAmount().intValue();
        if (!details.isEmpty()) {
            var d = details.get(0);
            productName = d.getProduct() != null ? d.getProduct().getName() : "Hoa tươi";
            quantity = d.getQuantity(); price = d.getPrice().intValue();
        }

        String toName = o.getRecipientName() != null && !o.getRecipientName().isBlank() ? o.getRecipientName() : o.getCustomer().getFullName();
        if (toName == null || toName.isBlank()) toName = "Khách hàng";

        String toPhone = o.getRecipientPhone() != null && !o.getRecipientPhone().isBlank() ? o.getRecipientPhone() : o.getCustomer().getPhone();
        if (toPhone == null || toPhone.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số điện thoại người nhận không được để trống.");
        }

        String fromName = o.getShop().getName() != null && !o.getShop().getName().isBlank() ? o.getShop().getName() : "FlowerShop";
        String fromPhone = o.getShop().getOwner() != null && o.getShop().getOwner().getPhone() != null && !o.getShop().getOwner().getPhone().isBlank() ? o.getShop().getOwner().getPhone() : null;
        if (fromPhone == null || fromPhone.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số điện thoại của cửa hàng (hoặc chủ shop) không được để trống.");
        }

        String toFullAddress = (toAddr.getAddressLine() != null ? toAddr.getAddressLine() : "") + ", " +
                (toAddr.getWard() != null ? toAddr.getWard() : "") + ", " +
                (toAddr.getDistrict() != null ? toAddr.getDistrict() : "") + ", " +
                (toAddr.getCity() != null ? toAddr.getCity() : "");

        String fromFullAddress = (shopAddr.getAddressLine() != null ? shopAddr.getAddressLine() : "") + ", " +
                (shopAddr.getWard() != null ? shopAddr.getWard() : "") + ", " +
                (shopAddr.getDistrict() != null ? shopAddr.getDistrict() : "") + ", " +
                (shopAddr.getCity() != null ? shopAddr.getCity() : "");

        var result = ghn.createOrder(
                toName, toPhone,
                toFullAddress,
                toAddr.getGhnDistrictId(), toAddr.getGhnWardCode(),
                fromName, fromPhone,
                fromFullAddress,
                shopAddr.getGhnDistrictId(), shopAddr.getGhnWardCode(),
                o.getTotalAmount().intValue(), productName, quantity, price
        );

        Delivery d = Delivery.builder().id(UUID.randomUUID().toString()).order(o)
                .deliveryPartnerId("GHN").trackingCode(result.orderCode())
                .status(DeliveryStatus.PENDING).createdBy(actor).build();
        deliveries.save(d);
        o.setStatus(OrderStatus.DELIVERING); o.setLastModifyBy(actor);
        return new ShipResult(result.orderCode());
    }

    /**
     * Tự động phân giải địa chỉ dạng chuỗi (Tỉnh/Huyện/Xã) sang mã GHN tương ứng (ghnDistrictId, ghnWardCode).
     * Phương thức này phục vụ tương thích ngược cho các địa chỉ cũ trong database chưa có mã định danh GHN.
     * Quy trình:
     * 1. Gọi API GHN lấy danh sách tỉnh thành và tìm tỉnh khớp với tên thành phố.
     * 2. Lấy danh sách quận/huyện của tỉnh đó và tìm huyện khớp với tên quận/huyện.
     * 3. Lấy danh sách phường/xã của huyện đó và tìm xã khớp với tên phường/xã.
     * 4. Gán mã ghnDistrictId và ghnWardCode vào Entity Address. Nếu không tìm thấy, ném lỗi 400 yêu cầu cập nhật lại.
     */
    private void resolveGhnAddress(Address addr) {
        if (addr.getCity() == null || addr.getCity().isBlank()) return;

        try {
            var provinces = ghn.provinces();
            var matchedProvince = provinces.stream()
                    .filter(p -> p.name().equalsIgnoreCase(addr.getCity()) || addr.getCity().toLowerCase().contains(p.name().toLowerCase()) || p.name().toLowerCase().contains(addr.getCity().toLowerCase()))
                    .findFirst()
                    .orElse(null);

            if (matchedProvince != null && addr.getDistrict() != null && !addr.getDistrict().isBlank()) {
                int provId = Integer.parseInt(matchedProvince.id());
                var districts = ghn.districts(provId);
                var matchedDistrict = districts.stream()
                        .filter(dist -> dist.name().equalsIgnoreCase(addr.getDistrict()) || addr.getDistrict().toLowerCase().contains(dist.name().toLowerCase()) || dist.name().toLowerCase().contains(addr.getDistrict().toLowerCase()))
                        .findFirst()
                        .orElse(null);

                if (matchedDistrict != null) {
                    int distId = Integer.parseInt(matchedDistrict.id());
                    addr.setGhnDistrictId(distId);

                    if (addr.getWard() != null && !addr.getWard().isBlank()) {
                        var wards = ghn.wards(distId);
                        var matchedWard = wards.stream()
                                .filter(w -> w.name().equalsIgnoreCase(addr.getWard()) || addr.getWard().toLowerCase().contains(w.name().toLowerCase()) || w.name().toLowerCase().contains(addr.getWard().toLowerCase()))
                                .findFirst()
                                .orElse(null);

                        if (matchedWard != null) {
                            addr.setGhnWardCode(matchedWard.id());
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Không gán cứng giá trị mặc định để đảm bảo dữ liệu chính xác
        }
        
        if (addr.getGhnDistrictId() == null || addr.getGhnWardCode() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể tự động phân giải mã GHN cho địa chỉ này. Vui lòng cập nhật lại địa chỉ với hệ thống GHN.");
        }
    }

    /**
     * Đồng bộ trạng thái vận đơn thực tế từ API GHN về hệ thống.
     * Nếu GHN báo đã giao (DELIVERED): Tự động chuyển đơn hàng sang COMPLETED và cập nhật trạng thái thanh toán sang SUCCESS.
     */
    @Transactional
    public DeliveryStatus refreshStatus(String shopId, String actor, String orderId) {
        owned(shopId, actor);
        Order o = orders.findByIdAndShopId(orderId, shopId).orElseThrow(ManagerOrderService::notFound);
        Delivery d = deliveries.findByOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Đơn hàng chưa có vận đơn GHN."));
        DeliveryStatus status = ghn.checkStatus(d.getTrackingCode());
        d.setStatus(status); d.setLastModifyBy(actor);
        if (status == DeliveryStatus.DELIVERED) {
            o.setStatus(OrderStatus.COMPLETED); o.setLastModifyBy(actor);
            payments.findByOrderId(orderId).forEach(p -> { p.setStatus(PaymentStatus.SUCCESS); p.setLastModifyBy(actor); });
        }
        return status;
    }

    /**
     * Giả lập giao hàng thành công trên môi trường thử nghiệm (Sandbox / Dev).
     * Gọi endpoint đổi trạng thái của GHN Sandbox, đồng thời cập nhật trạng thái đơn thành COMPLETED.
     */
    @Transactional
    public DeliveryStatus simulateDelivered(String shopId, String actor, String orderId) {
        owned(shopId, actor);
        Order o = orders.findByIdAndShopId(orderId, shopId).orElseThrow(ManagerOrderService::notFound);
        Delivery d = deliveries.findByOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Đơn hàng chưa có vận đơn GHN."));

        try {
            ghn.switchStatus(d.getTrackingCode(), "delivered");
        } catch (Exception e) {
            // Ghi log nếu cần; tiếp tục cập nhật cơ sở dữ liệu nội bộ
        }

        d.setStatus(DeliveryStatus.DELIVERED);
        d.setLastModifyBy(actor);
        deliveries.save(d);

        o.setStatus(OrderStatus.COMPLETED);
        o.setLastModifyBy(actor);
        orders.save(o);

        payments.findByOrderId(orderId).forEach(p -> {
            p.setStatus(PaymentStatus.SUCCESS);
            p.setLastModifyBy(actor);
        });

        return DeliveryStatus.DELIVERED;
    }

    /** DTO chứa số lượng đơn hàng cần xử lý */
    public record QueueCount(long pendingCount) {}

    /** DTO chứa lý do hủy đơn hàng */
    public record CancelRequest(String reason) {}

    /**
     * Lấy danh sách hàng đợi xử lý của shop (chỉ bao gồm các đơn PENDING, AWAITING_DEPOSIT, PROCESSING).
     */
    public Results queue(String shopId, String actor, int page, String status) {
        owned(shopId, actor);
        Pageable pageable = PageRequest.of(page, 20, Sort.by(Sort.Order.desc("createdDate"), Sort.Order.asc("id")));
        Page<Order> result;
        List<OrderStatus> queueStatuses = List.of(OrderStatus.PENDING, OrderStatus.AWAITING_DEPOSIT, OrderStatus.PROCESSING);
        
        if (status != null && !status.isBlank()) {
            try {
                OrderStatus st = OrderStatus.valueOf(status.toUpperCase());
                if (!queueStatuses.contains(st)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trạng thái không thuộc danh sách chờ xử lý.");
                }
                result = orders.findByShopIdAndStatus(shopId, st, pageable);
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trạng thái không hợp lệ: " + status);
            }
        } else {
            result = orders.findByShopIdAndStatusIn(shopId, queueStatuses, pageable);
        }
        var content = result.map(o -> new OrderSummary(o.getId(),
                o.getCustomer().getFullName(), o.getCustomer().getPhone(),
                o.getTotalAmount(), o.getStatus().name(), o.getCreatedDate())).toList();
        return new Results(content, page, result.getTotalPages(), result.getTotalElements());
    }

    /**
     * Đếm tổng số đơn hàng đang chờ xác nhận hoặc chờ đặt cọc.
     */
    public QueueCount queueCount(String shopId, String actor) {
        owned(shopId, actor);
        long count = orders.countByShopIdAndStatusIn(shopId, List.of(OrderStatus.PENDING, OrderStatus.AWAITING_DEPOSIT));
        return new QueueCount(count);
    }

    /**
     * Hủy đơn hàng trước khi chuyển sang giai đoạn giao hàng.
     * Chỉ cho phép hủy khi đơn ở trạng thái PENDING, AWAITING_DEPOSIT hoặc PROCESSING.
     */
    @Transactional
    public void cancel(String shopId, String actor, String orderId, String reason) {
        owned(shopId, actor);
        Order o = orders.findByIdAndShopId(orderId, shopId).orElseThrow(ManagerOrderService::notFound);
        if (o.getStatus() != OrderStatus.PENDING && o.getStatus() != OrderStatus.AWAITING_DEPOSIT && o.getStatus() != OrderStatus.PROCESSING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ có thể hủy đơn hàng trước khi bắt đầu giao.");
        }
        o.setStatus(OrderStatus.CANCELLED);
        o.setCancelReason(reason);
        o.setLastModifyBy(actor);
    }

    /**
     * Helper tổng hợp chi tiết đơn hàng (OrderDetailResponse) đầy đủ thông tin:
     * - Sản phẩm trong đơn (tên, số lượng, giá).
     * - Địa chỉ người nhận và số điện thoại người nhận.
     * - Địa chỉ người gửi (Shop).
     * - Vận đơn đối tác giao hàng (mã vận đơn tracking code).
     * - Thông tin thanh toán (phương thức, trạng thái thanh toán).
     */
    private OrderDetailResponse buildDetail(Order o) {
        var items = orderDetails.findByOrderId(o.getId()).stream()
                .map(d -> new OrderItemResponse(d.getProduct() != null ? d.getProduct().getId() : null,
                        d.getProduct() != null ? d.getProduct().getName() : null, d.getPrice(), d.getQuantity()))
                .toList();
        DeliveryInfo di = deliveries.findByOrderId(o.getId())
                .map(d -> new DeliveryInfo(d.getTrackingCode(), d.getDeliveryPartnerId(), d.getStatus().name())).orElse(null);
        PaymentInfo pi = payments.findByOrderId(o.getId()).stream().findFirst()
                .map(p -> new PaymentInfo(p.getId(), p.getPaymentMethod().name(), p.getStatus().name(), p.getAmount())).orElse(null);
        
        // Định dạng địa chỉ người nhận
        var a = o.getDeliveryAddress();
        String toAddr = a != null ? (a.getAddressLine() != null ? a.getAddressLine() : "") + ", " + 
                                    (a.getWard() != null ? a.getWard() : "") + ", " + 
                                    (a.getDistrict() != null ? a.getDistrict() : "") + ", " + 
                                    (a.getCity() != null ? a.getCity() : "") : "";
        toAddr = toAddr.replaceAll("null, ", "").replaceAll("^, ", "").replaceAll(", $", "");

        // Định dạng địa chỉ gửi hàng của Shop
        String fromAddr = "";
        if (o.getShop() != null) {
            var shopAddrs = addresses.findByShopIdAndUserIsNullOrderByCreatedDateAscIdAsc(o.getShop().getId());
            Address shopAddr = shopAddrs.stream().filter(addr -> Boolean.TRUE.equals(addr.getIsDefault())).findFirst()
                    .orElse(shopAddrs.isEmpty() ? null : shopAddrs.get(0));
            if (shopAddr != null) {
                fromAddr = (shopAddr.getAddressLine() != null ? shopAddr.getAddressLine() : "") + ", " + 
                           (shopAddr.getWard() != null ? shopAddr.getWard() : "") + ", " + 
                           (shopAddr.getDistrict() != null ? shopAddr.getDistrict() : "") + ", " + 
                           (shopAddr.getCity() != null ? shopAddr.getCity() : "");
                fromAddr = fromAddr.replaceAll("null, ", "").replaceAll("^, ", "").replaceAll(", $", "");
            }
        }

        String toName = o.getRecipientName() != null && !o.getRecipientName().isBlank() ? o.getRecipientName() : (o.getCustomer() != null ? o.getCustomer().getFullName() : "Khách hàng");
        String toPhone = o.getRecipientPhone() != null && !o.getRecipientPhone().isBlank() ? o.getRecipientPhone() : (o.getCustomer() != null ? o.getCustomer().getPhone() : "");

        return new OrderDetailResponse(o.getId(), o.getCustomer() != null ? o.getCustomer().getId() : null, 
                o.getCustomer() != null ? o.getCustomer().getFullName() : null,
                o.getCustomer() != null ? o.getCustomer().getPhone() : null,
                toName, toPhone, toAddr, fromAddr, o.getStatus().name(), 
                o.getSubTotal(), o.getShippingFee(), o.getTotalAmount(), 
                o.getCreatedDate(), null, items, di, pi);
    }
}