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
    public void confirm(String shopId, String actor, String orderId) {
        owned(shopId, actor);
        Order o = orders.findByIdAndShopId(orderId, shopId).orElseThrow(ManagerOrderService::notFound);
        if (o.getStatus() != OrderStatus.PENDING)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ đơn hàng PENDING mới có thể xác nhận.");
        o.setStatus(OrderStatus.PROCESSING);
        o.setLastModifyBy(actor);
    }

    @Transactional
    public ShipResult ship(String shopId, String actor, String orderId) {
        owned(shopId, actor);
        Order o = orders.findByIdAndShopId(orderId, shopId).orElseThrow(ManagerOrderService::notFound);
        if (o.getStatus() != OrderStatus.PROCESSING)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ đơn hàng PROCESSING mới có thể gửi GHN.");

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
            // Log error if needed, but do not assign hardcoded fallback values
        }
        
        if (addr.getGhnDistrictId() == null || addr.getGhnWardCode() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể tự động phân giải mã GHN cho địa chỉ này. Vui lòng cập nhật lại địa chỉ với hệ thống GHN.");
        }
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
        }
        return status;
    }

    @Transactional
    public DeliveryStatus simulateDelivered(String shopId, String actor, String orderId) {
        owned(shopId, actor);
        Order o = orders.findByIdAndShopId(orderId, shopId).orElseThrow(ManagerOrderService::notFound);
        Delivery d = deliveries.findByOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Đơn hàng chưa có vận đơn GHN."));

        try {
            ghn.switchStatus(d.getTrackingCode(), "delivered");
        } catch (Exception e) {
            // Log if needed
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
        if (o.getStatus() != OrderStatus.PENDING && o.getStatus() != OrderStatus.AWAITING_DEPOSIT && o.getStatus() != OrderStatus.PROCESSING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ có thể hủy đơn hàng trước khi bắt đầu giao.");
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