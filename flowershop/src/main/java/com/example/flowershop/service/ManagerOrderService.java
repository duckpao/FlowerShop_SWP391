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
public class ManagerOrderService {

    public record OrderSummary(String id, String customerName, String customerPhone,
                               BigDecimal totalAmount, String status, java.time.LocalDateTime createdDate) {}

    public record OrderDetailResponse(String id, String customerId, String customerName, String customerPhone,
                                      String deliveryAddress, String status, BigDecimal subTotal,
                                      BigDecimal shippingFee, BigDecimal totalAmount,
                                      java.time.LocalDateTime createdDate, String note,
                                      List<OrderItemResponse> items, DeliveryInfo delivery, PaymentInfo payment) {}

    public record OrderItemResponse(String productId, String productName, BigDecimal price, Integer quantity) {}

    public record DeliveryInfo(String trackingCode, String deliveryPartnerId, String status) {}

    public record PaymentInfo(String id, String method, String status, BigDecimal amount) {}

    public record Results(List<OrderSummary> content, int page, int totalPages, long totalElements) {}

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
private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng.");
    }

    private Shop owned(String shopId, String actor) {
        Shop s = shops.findById(shopId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy shop."));
        if (!s.getOwner().getId().equals(actor))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không sở hữu shop này.");
        return s;
    }

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

    public OrderDetailResponse detail(String shopId, String actor, String orderId) {
        owned(shopId, actor);
        Order o = orders.findByIdAndShopId(orderId, shopId).orElseThrow(ManagerOrderService::notFound);
        return buildDetail(o);
    }
@Transactional
    public ShipResult ship(String shopId, String actor, String orderId) {
        owned(shopId, actor);
        Order o = orders.findByIdAndShopId(orderId, shopId).orElseThrow(ManagerOrderService::notFound);
        if (o.getStatus() != OrderStatus.PENDING)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ đơn hàng PENDING mới có thể gửi GHN.");

        var shopAddrs = addresses.findByShopIdAndUserIsNullOrderByCreatedDateAscIdAsc(shopId);
        Address shopAddr = shopAddrs.stream().filter(a -> Boolean.TRUE.equals(a.getIsDefault())).findFirst()
                .orElse(shopAddrs.isEmpty() ? null : shopAddrs.get(0));
        if (shopAddr == null || shopAddr.getGhnDistrictId() == null || shopAddr.getGhnWardCode() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Shop chưa cấu hình địa chỉ GHN.");

        Address toAddr = o.getDeliveryAddress();
        if (toAddr.getGhnDistrictId() == null || toAddr.getGhnWardCode() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Địa chỉ nhận chưa có mã GHN.");

        var details = orderDetails.findByOrderId(orderId);
        String productName = "Sản phẩm"; int quantity = 0, price = 0;
        if (!details.isEmpty()) {
            var d = details.get(0);
            productName = d.getProduct() != null ? d.getProduct().getName() : "Sản phẩm";
            quantity = d.getQuantity(); price = d.getPrice().intValue();
        }

        var result = ghn.createOrder(
                o.getCustomer().getFullName(), o.getCustomer().getPhone(),
                toAddr.getAddressLine() + ", " + toAddr.getWard() + ", " + toAddr.getDistrict() + ", " + toAddr.getCity(),
                toAddr.getGhnDistrictId(), toAddr.getGhnWardCode(),
                o.getShop().getName(), o.getShop().getOwner().getPhone(),
                shopAddr.getAddressLine() + ", " + shopAddr.getWard() + ", " + shopAddr.getDistrict() + ", " + shopAddr.getCity(),
                shopAddr.getGhnDistrictId(), shopAddr.getGhnWardCode(),
                o.getTotalAmount().intValue(), productName, quantity, price
        );

        Delivery d = Delivery.builder().id(UUID.randomUUID().toString()).order(o)
                .deliveryPartnerId("GHN").trackingCode(result.orderCode())
                .status(DeliveryStatus.PENDING).createdBy(actor).build();
        deliveries.save(d);
        o.setStatus(OrderStatus.PROCESSING); o.setLastModifyBy(actor);
        return new ShipResult(result.orderCode());
    }

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
        } else if (status == DeliveryStatus.ON_THE_WAY || status == DeliveryStatus.PICKED_UP) {
            if (o.getStatus() == OrderStatus.PROCESSING) o.setStatus(OrderStatus.DELIVERING);
            o.setLastModifyBy(actor);
        }
        return status;
    }

    public record QueueCount(long pendingCount) {}
    public record CancelRequest(String reason) {}

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

    public QueueCount queueCount(String shopId, String actor) {
        owned(shopId, actor);
        long count = orders.countByShopIdAndStatusIn(shopId, List.of(OrderStatus.PENDING, OrderStatus.AWAITING_DEPOSIT));
        return new QueueCount(count);
    }

    @Transactional
    public void cancel(String shopId, String actor, String orderId, String reason) {
        owned(shopId, actor);
        Order o = orders.findByIdAndShopId(orderId, shopId).orElseThrow(ManagerOrderService::notFound);
        if (o.getStatus() != OrderStatus.PENDING && o.getStatus() != OrderStatus.AWAITING_DEPOSIT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ có thể hủy đơn hàng đang chờ xử lý.");
        }
        o.setStatus(OrderStatus.CANCELLED);
        o.setCancelReason(reason);
        o.setLastModifyBy(actor);
    }

    private OrderDetailResponse buildDetail(Order o) {
        var items = orderDetails.findByOrderId(o.getId()).stream()
                .map(d -> new OrderItemResponse(d.getProduct() != null ? d.getProduct().getId() : null,
                        d.getProduct() != null ? d.getProduct().getName() : null, d.getPrice(), d.getQuantity()))
                .toList();
        DeliveryInfo di = deliveries.findByOrderId(o.getId())
                .map(d -> new DeliveryInfo(d.getTrackingCode(), d.getDeliveryPartnerId(), d.getStatus().name())).orElse(null);
        PaymentInfo pi = payments.findByOrderId(o.getId()).stream().findFirst()
                .map(p -> new PaymentInfo(p.getId(), p.getPaymentMethod().name(), p.getStatus().name(), p.getAmount())).orElse(null);
        var a = o.getDeliveryAddress();
        String addr = a != null ? a.getAddressLine() + ", " + a.getWard() + ", " + a.getDistrict() + ", " + a.getCity() : "";
        return new OrderDetailResponse(o.getId(), o.getCustomer().getId(), o.getCustomer().getFullName(),
                o.getCustomer().getPhone(), addr, o.getStatus().name(), o.getSubTotal(), o.getShippingFee(),
                o.getTotalAmount(), o.getCreatedDate(), null, items, di, pi);
    }
}