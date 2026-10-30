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

@Service
@Transactional(readOnly = true)
public class CustomerOrderService {

    public record OrderLine(String productId, String productName, String imageUrl, BigDecimal price, Integer quantity) {}

    /** paymentMethod: COD | ONLINE | null; paymentStatus: SUCCESS nếu đã trả, ngược lại trạng thái giao dịch gần nhất.
     *  paymentSecondsLeft: số giây còn lại để thanh toán đơn online chưa trả (null nếu không áp dụng); tính ở server để khỏi lệch múi giờ. */
    public record OrderSummary(String id, String shopName, BigDecimal totalAmount, String status,
                               java.time.LocalDateTime createdDate, Long itemCount,
                               String shopId, BigDecimal subTotal, BigDecimal shippingFee,
                               String paymentMethod, String paymentStatus, List<OrderLine> items,
                               Long paymentSecondsLeft, String checkoutGroup) {}

    public record OrderDetailResponse(String id, String shopId, String shopName, String status,
                                      BigDecimal subTotal, BigDecimal shippingFee, BigDecimal totalAmount,
                                      java.time.LocalDateTime createdDate, String note,
                                      List<OrderItemResponse> items, DeliveryInfo delivery,
                                      String orderType, BigDecimal discountAmount, String recipientName,
                                      String recipientPhone, String deliveryAddress,
                                      List<PaymentLine> payments, List<RefundLine> refunds) {}

    /** Một lần thanh toán của đơn (cọc / phần còn lại / toàn bộ), dùng cho hóa đơn. */
    public record PaymentLine(String paymentType, String paymentMethod, BigDecimal amount, String status,
                              String invoiceNumber, String gatewayTransactionNo, java.time.LocalDateTime paidAt) {}

    /** Khoản hoàn tiền của đơn để khách theo dõi: REQUESTED → APPROVED → PROCESSED, hoặc REJECTED. */
    public record RefundLine(BigDecimal amount, String reason, String status,
                             java.time.LocalDateTime createdDate, java.time.LocalDateTime updatedDate) {}

        public record ShippingFeeResponse(BigDecimal shippingFee) {}

    public record OrderItemResponse(String productId, String productName, BigDecimal price, Integer quantity) {}

    public record DeliveryInfo(String trackingCode, String deliveryPartnerId, String status, String ghnStatus) {}

    public record Results(List<OrderSummary> content, int page, int totalPages, long totalElements) {}

    private final AddressRepository addresses;
    private final OrderRepository orders;
    private final OrderDetailRepository orderDetails;
    private final PaymentRepository payments;
    private final DeliveryRepository deliveries;
    private final GhnService ghn;
    private final ProductImageRepository productImages;
    private final DeliverySync deliverySync;
    private final RefundRepository refunds;

    public CustomerOrderService(AddressRepository addresses,
                                OrderRepository orders, OrderDetailRepository orderDetails,
                                PaymentRepository payments, DeliveryRepository deliveries,
                                GhnService ghn,
                                ProductImageRepository productImages, DeliverySync deliverySync,
                                RefundRepository refunds) {
        this.refunds = refunds;
        this.addresses = addresses;
        this.orders = orders;
        this.orderDetails = orderDetails;
        this.payments = payments;
        this.deliveries = deliveries;
        this.ghn = ghn;
        this.productImages = productImages;
        this.deliverySync = deliverySync;
    }
private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy dữ liệu.");
    }

    /** Fee from the shop's pickup address to the customer's address. */
    private BigDecimal quote(Shop shop, Address to) {
        Address from = pickupAddress(shop);
        return ghn.calculateFee(from.getGhnDistrictId(), from.getGhnWardCode(),
                to.getGhnDistrictId(), to.getGhnWardCode(), GhnService.DEFAULT_WEIGHT_GRAM);
    }

    /** The shop's default (or first) address; it must carry GHN codes to be a pickup point. */
    private Address pickupAddress(Shop shop) {
        List<Address> list = addresses.findByShopIdAndUserIsNullOrderByCreatedDateAscIdAsc(shop.getId());
        Address address = list.stream().filter(a -> Boolean.TRUE.equals(a.getIsDefault())).findFirst()
                .orElse(list.isEmpty() ? null : list.get(0));
        if (address == null || address.getGhnDistrictId() == null || address.getGhnWardCode() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cửa hàng chưa có địa chỉ lấy hàng theo danh mục GHN.");
        return address;
    }

    /** @param status lọc theo trạng thái đơn (null hoặc rỗng = tất cả). */
    public Results list(String customerId, int page, String status) {
        Pageable pageable = PageRequest.of(page, 20, Sort.by(Sort.Order.desc("createdDate"), Sort.Order.asc("id")));
        Page<Order> result;
        if (status == null || status.isBlank()) {
            result = orders.findByCustomerIdOrderByCreatedDateDesc(customerId, pageable);
        } else {
            OrderStatus st;
            try {
                st = OrderStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trạng thái không hợp lệ: " + status);
            }
            result = orders.findByCustomerIdAndStatusOrderByCreatedDateDesc(customerId, st, pageable);
        }
        var content = result.getContent().stream().map(this::summary).toList();
        return new Results(content, page, result.getTotalPages(), result.getTotalElements());
    }

    /** Các đơn của khách theo danh sách id (bỏ qua id không tồn tại hoặc không thuộc khách), theo thứ tự id truyền vào. */
    public List<OrderSummary> byIds(String customerId, List<String> ids) {
        return ids.stream().distinct().limit(20)
                .map(id -> orders.findById(id).filter(o -> o.getCustomer().getId().equals(customerId)))
                .flatMap(Optional::stream).map(this::summary).toList();
    }

    private OrderSummary summary(Order o) {
        var details = orderDetails.findByOrderId(o.getId());
        var productIds = details.stream().filter(d -> d.getProduct() != null).map(d -> d.getProduct().getId()).distinct().toList();
        Map<String, String> imageByProduct = new HashMap<>();
        if (!productIds.isEmpty()) {
            productImages.findByProductIdInOrderByDisplayOrderAscIdAsc(productIds)
                    .forEach(img -> imageByProduct.putIfAbsent(img.getProduct().getId(), img.getImageUrl()));
        }
        var lines = details.stream().map(d -> new OrderLine(
                d.getProduct() != null ? d.getProduct().getId() : null,
                d.getProduct() != null ? d.getProduct().getName() : null,
                d.getProduct() != null ? imageByProduct.get(d.getProduct().getId()) : null,
                d.getPrice(), d.getQuantity())).toList();
        var orderPayments = payments.findByOrderId(o.getId());
        String method = orderPayments.stream().anyMatch(p -> p.getPaymentMethod() == PaymentMethod.COD) ? "COD"
                : orderPayments.stream().anyMatch(p -> p.getPaymentMethod() == PaymentMethod.ONLINE) ? "ONLINE"
                // Đơn online vừa tạo chưa có bản ghi thanh toán nào (chỉ đơn COD luôn có sẵn); coi là online đang chờ trả.
                : o.getStatus() == OrderStatus.PENDING ? "ONLINE" : null;
        String paymentStatus = orderPayments.stream().anyMatch(p -> p.getStatus() == PaymentStatus.SUCCESS) ? "SUCCESS"
                : orderPayments.stream().max(Comparator.comparing(Payment::getCreatedDate, Comparator.nullsFirst(Comparator.naturalOrder())))
                        .map(p -> p.getStatus().name()).orElse(null);
        Long secondsLeft = null;
        if (o.getStatus() == OrderStatus.PENDING && "ONLINE".equals(method) && !"SUCCESS".equals(paymentStatus)
                && o.getCreatedDate() != null) {
            long elapsed = java.time.Duration.between(o.getCreatedDate(), java.time.LocalDateTime.now()).getSeconds();
            secondsLeft = Math.max(0, SePayGatewayService.PAYMENT_WINDOW_MINUTES * 60 - elapsed);
        }
        return new OrderSummary(o.getId(), o.getShop().getName(), o.getTotalAmount(), o.getStatus().name(),
                o.getCreatedDate(), (long) lines.size(), o.getShop().getId(), o.getSubTotal(), o.getShippingFee(),
                method, paymentStatus, lines, secondsLeft, o.getCheckoutGroup());
    }

    public OrderDetailResponse detail(String customerId, String orderId) {
        Order order = orders.findById(orderId)
                .filter(o -> o.getCustomer().getId().equals(customerId))
                .orElseThrow(CustomerOrderService::notFound);
        var items = orderDetails.findByOrderId(orderId).stream()
                .map(d -> new OrderItemResponse(
                        d.getProduct() != null ? d.getProduct().getId() : null,
                        d.getProduct() != null ? d.getProduct().getName() : null,
                        d.getPrice(), d.getQuantity()))
                .toList();
        DeliveryInfo delivery = deliveries.findByOrderId(orderId)
                .map(d -> new DeliveryInfo(d.getTrackingCode(), d.getDeliveryPartnerId(), d.getStatus().name(), d.getGhnStatus()))
                .orElse(null);
        Address a = order.getDeliveryAddress();
        String address = a == null ? null : String.join(", ", java.util.stream.Stream.of(
                a.getAddressLine(), a.getWard(), a.getDistrict(), a.getCity()).filter(s -> s != null && !s.isBlank()).toList());
        var paymentLines = payments.findByOrderId(orderId).stream()
                .sorted(Comparator.comparing(Payment::getCreatedDate, Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(p -> new PaymentLine(p.getPaymentType().name(), p.getPaymentMethod().name(), p.getAmount(),
                        p.getStatus().name(), p.getInvoiceNumber(), p.getGatewayTransactionNo(),
                        p.getStatus() == PaymentStatus.SUCCESS ? p.getLastModifyDate() : null))
                .toList();
        var refundLines = refunds.findByOrderIdOrderByCreatedDateAsc(orderId).stream()
                .map(r -> new RefundLine(r.getAmount(), r.getReason(), r.getStatus().name(), r.getCreatedDate(), r.getLastModifyDate()))
                .toList();
        return new OrderDetailResponse(order.getId(), order.getShop().getId(), order.getShop().getName(),
                order.getStatus().name(), order.getSubTotal(), order.getShippingFee(), order.getTotalAmount(),
                order.getCreatedDate(), order.getCustomerNote(), items, delivery,
                order.getOrderType() == null ? null : order.getOrderType().name(), order.getDiscountAmount(),
                order.getRecipientName(), order.getRecipientPhone(), address, paymentLines, refundLines);
    }

        public BigDecimal calculateShippingFeeForOrder(String customerId, String orderId) {
                Order order = orders.findById(orderId)
                                .filter(o -> o.getCustomer().getId().equals(customerId))
                                .orElseThrow(CustomerOrderService::notFound);
                Address destination = order.getDeliveryAddress();
                if (destination == null || destination.getGhnDistrictId() == null || destination.getGhnWardCode() == null) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Địa chỉ nhận hàng chưa có thông tin GHN hợp lệ.");
                }
                return quote(order.getShop(), destination);
        }

        @Transactional
        public DeliveryInfo refreshTracking(String customerId, String orderId) {
                Order order = orders.findById(orderId)
                                .filter(o -> o.getCustomer().getId().equals(customerId))
                                .orElseThrow(CustomerOrderService::notFound);
                if (order.getStatus() != OrderStatus.DELIVERING && order.getStatus() != OrderStatus.COMPLETED
                                && order.getStatus() != OrderStatus.REFUNDED) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "Đơn hàng chưa được bàn giao cho đơn vị vận chuyển.");
                }

                Delivery delivery = deliveries.findByOrderId(orderId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Đơn hàng chưa có vận đơn."));
                deliverySync.apply(order, delivery, ghn.track(delivery.getTrackingCode()), customerId);
                return new DeliveryInfo(delivery.getTrackingCode(), delivery.getDeliveryPartnerId(),
                                delivery.getStatus().name(), delivery.getGhnStatus());
        }
}