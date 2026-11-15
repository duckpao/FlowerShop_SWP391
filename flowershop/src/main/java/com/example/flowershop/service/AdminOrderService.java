package com.example.flowershop.service;

import com.example.flowershop.entity.Order;
import com.example.flowershop.entity.Payment;
import com.example.flowershop.entity.enums.OrderStatus;
import com.example.flowershop.entity.enums.PaymentStatus;
import com.example.flowershop.repository.*;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

/** Admin xem đơn hàng toàn hệ thống (chỉ đọc). */
@Service
@Transactional(readOnly = true)
public class AdminOrderService {

    public record OrderRow(String id, String customerName, String shopId, String shopName, BigDecimal totalAmount,
                           String status, String orderType, LocalDateTime createdDate, String couponId) {}

    public record OrderPage(List<OrderRow> content, int page, int size, long totalElements, int totalPages) {}

    public record AdminOrderDetail(String id, String status, String orderType, LocalDateTime createdDate,
                                   String customerId, String customerName, String customerEmail, String customerPhone,
                                   String shopId, String shopName,
                                   String recipientName, String recipientPhone, String deliveryAddress,
                                   BigDecimal subTotal, BigDecimal discountAmount, BigDecimal shippingFee,
                                   BigDecimal totalAmount, BigDecimal depositAmount,
                                   String couponCode, String customerNote, String shopNote, String cancelReason,
                                   List<CustomerOrderService.OrderItemResponse> items,
                                   CustomerOrderService.DeliveryInfo delivery,
                                   List<CustomerOrderService.PaymentLine> payments,
                                   List<CustomerOrderService.RefundLine> refunds) {}

    private final OrderRepository orders;
    private final OrderDetailRepository orderDetails;
    private final PaymentRepository payments;
    private final DeliveryRepository deliveries;
    private final RefundRepository refunds;
    private final CouponRepository coupons;

    public AdminOrderService(OrderRepository orders, OrderDetailRepository orderDetails, PaymentRepository payments,
                             DeliveryRepository deliveries, RefundRepository refunds, CouponRepository coupons) {
        this.orders = orders;
        this.orderDetails = orderDetails;
        this.payments = payments;
        this.deliveries = deliveries;
        this.refunds = refunds;
        this.coupons = coupons;
    }

    private static String like(String raw) {
        return "%" + raw.strip().toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }

    private static LocalDate date(String value, String label) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value.strip());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(label + " không hợp lệ, cần dạng yyyy-MM-dd.");
        }
    }

    public OrderPage list(String status, String q, String from, String to, int page, int size) {
        if (page < 0 || page > 100000 || size < 1 || size > 100 || (q != null && q.length() > 100))
            throw new IllegalArgumentException("Trang phải từ 0 đến 100000, kích thước từ 1 đến 100, từ khóa tối đa 100 ký tự.");
        OrderStatus st = null;
        if (status != null && !status.isBlank()) {
            try {
                st = OrderStatus.valueOf(status.strip().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Trạng thái không hợp lệ.");
            }
        }
        LocalDate fromDate = date(from, "Ngày bắt đầu");
        LocalDate toDate = date(to, "Ngày kết thúc");
        if (fromDate != null && toDate != null && toDate.isBefore(fromDate))
            throw new IllegalArgumentException("Ngày kết thúc phải sau ngày bắt đầu.");
        final OrderStatus selected = st;
        String term = q == null || q.isBlank() ? null : like(q);
        Specification<Order> spec = (root, cq, cb) -> {
            var customer = root.join("customer");
            var shop = root.join("shop");
            var predicates = new ArrayList<Predicate>();
            if (selected != null) predicates.add(cb.equal(root.get("status"), selected));
            if (fromDate != null) predicates.add(cb.greaterThanOrEqualTo(root.<LocalDateTime>get("createdDate"), fromDate.atStartOfDay()));
            if (toDate != null) predicates.add(cb.lessThan(root.<LocalDateTime>get("createdDate"), toDate.plusDays(1).atStartOfDay()));
            if (term != null) predicates.add(cb.or(
                    cb.like(cb.lower(root.<String>get("id")), term, '!'),
                    cb.like(cb.lower(customer.<String>get("fullName")), term, '!'),
                    cb.like(cb.lower(shop.<String>get("name")), term, '!')));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        var result = orders.findAll(spec, PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("createdDate"), Sort.Order.asc("id"))));
        var rows = result.getContent().stream().map(o -> new OrderRow(o.getId(), o.getCustomer().getFullName(),
                o.getShop().getId(), o.getShop().getName(), o.getTotalAmount(), o.getStatus().name(),
                o.getOrderType() == null ? null : o.getOrderType().name(), o.getCreatedDate(), o.getCouponId())).toList();
        return new OrderPage(rows, page, size, result.getTotalElements(), result.getTotalPages());
    }

    public AdminOrderDetail detail(String id) {
        Order o = orders.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng."));
        var items = orderDetails.findByOrderId(id).stream().map(d -> new CustomerOrderService.OrderItemResponse(
                d.getProduct() != null ? d.getProduct().getId() : null,
                d.getProduct() != null ? d.getProduct().getName() : null, d.getPrice(), d.getQuantity())).toList();
        var delivery = deliveries.findByOrderId(id).map(d -> new CustomerOrderService.DeliveryInfo(d.getTrackingCode(),
                d.getDeliveryPartnerId(), d.getStatus().name(), d.getGhnStatus())).orElse(null);
        var a = o.getDeliveryAddress();
        String address = a == null ? null : String.join(", ", java.util.stream.Stream.of(
                a.getAddressLine(), a.getWard(), a.getDistrict(), a.getCity()).filter(s -> s != null && !s.isBlank()).toList());
        var paymentLines = payments.findByOrderId(id).stream()
                .sorted(Comparator.comparing(Payment::getCreatedDate, Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(p -> new CustomerOrderService.PaymentLine(p.getPaymentType().name(), p.getPaymentMethod().name(),
                        p.getAmount(), p.getStatus().name(), p.getInvoiceNumber(), p.getGatewayTransactionNo(),
                        p.getStatus() == PaymentStatus.SUCCESS ? p.getLastModifyDate() : null)).toList();
        var refundLines = refunds.findByOrderIdOrderByCreatedDateAsc(id).stream()
                .map(r -> new CustomerOrderService.RefundLine(r.getAmount(), r.getReason(), r.getStatus().name(),
                        r.getCreatedDate(), r.getLastModifyDate(), r.getId(), r.getDescription(),
                        CustomerRefundService.images(r), r.getShopResponse())).toList();
        String couponCode = o.getCouponId() == null ? null
                : coupons.findById(o.getCouponId()).map(c -> c.getCode()).orElse(null);
        var c = o.getCustomer();
        return new AdminOrderDetail(o.getId(), o.getStatus().name(), o.getOrderType() == null ? null : o.getOrderType().name(),
                o.getCreatedDate(), c.getId(), c.getFullName(), c.getEmail(), c.getPhone(),
                o.getShop().getId(), o.getShop().getName(), o.getRecipientName(), o.getRecipientPhone(), address,
                o.getSubTotal(), o.getDiscountAmount(), o.getShippingFee(), o.getTotalAmount(), o.getDepositAmount(),
                couponCode, o.getCustomerNote(), o.getShopNote(), o.getCancelReason(),
                items, delivery, paymentLines, refundLines);
    }
}
