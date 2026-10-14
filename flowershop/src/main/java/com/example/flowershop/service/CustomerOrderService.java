package com.example.flowershop.service;

import com.example.flowershop.entity.*;
import com.example.flowershop.entity.enums.*;
import com.example.flowershop.repository.*;
import jakarta.validation.constraints.*;
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

    public record CheckoutRequest(
            @NotBlank String addressId,
            @NotBlank String productId,
            @NotNull @Min(1) @Max(1000) Integer quantity,
            @Size(max=500) String note) {}

    public record CheckoutResponse(String orderId, BigDecimal subTotal, BigDecimal shippingFee,
                                   BigDecimal totalAmount, String status) {}

    public record OrderSummary(String id, String shopName, BigDecimal totalAmount, String status,
                               java.time.LocalDateTime createdDate, Long itemCount) {}

    public record OrderDetailResponse(String id, String shopId, String shopName, String status,
                                      BigDecimal subTotal, BigDecimal shippingFee, BigDecimal totalAmount,
                                      java.time.LocalDateTime createdDate, String note,
                                      List<OrderItemResponse> items, DeliveryInfo delivery) {}

        public record ShippingFeeResponse(BigDecimal shippingFee) {}

    public record OrderItemResponse(String productId, String productName, BigDecimal price, Integer quantity) {}

    public record DeliveryInfo(String trackingCode, String deliveryPartnerId, String status, String ghnStatus) {}

    public record Results(List<OrderSummary> content, int page, int totalPages, long totalElements) {}

    private final ProductRepository products;
    private final AddressRepository addresses;
    private final OrderRepository orders;
    private final OrderDetailRepository orderDetails;
    private final PaymentRepository payments;
    private final DeliveryRepository deliveries;
    private final UserRepository users;
    private final GhnService ghn;
    private final DeliverySync deliverySync;

    public CustomerOrderService(ProductRepository products, AddressRepository addresses,
                                OrderRepository orders, OrderDetailRepository orderDetails,
                                PaymentRepository payments, DeliveryRepository deliveries,
                                UserRepository users, GhnService ghn, DeliverySync deliverySync) {
        this.products = products;
        this.addresses = addresses;
        this.orders = orders;
        this.orderDetails = orderDetails;
        this.payments = payments;
        this.deliveries = deliveries;
        this.users = users;
        this.ghn = ghn;
        this.deliverySync = deliverySync;
    }
private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy dữ liệu.");
    }

    @Transactional
    public BigDecimal calculateFee(String customerId, CheckoutRequest req) {
        Product product = products.findById(req.productId())
                .filter(p -> p.getStatus() == ProductStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sản phẩm không bán nữa."));
        return quote(product.getShop(), destination(customerId, req.addressId()));
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

    /** The customer's saved address; it must carry the GHN district id and ward code a fee quote needs. */
    private Address destination(String customerId, String addressId) {
        Address address = addresses.findById(addressId)
                .filter(a -> a.getUser() != null && a.getUser().getId().equals(customerId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Địa chỉ không tồn tại hoặc không thuộc về bạn."));
        if (address.getGhnDistrictId() == null || address.getGhnWardCode() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Địa chỉ chưa có mã quận/huyện và phường/xã GHN. Hãy lưu lại địa chỉ trong hồ sơ.");
        return address;
    }

    @Transactional
    public CheckoutResponse checkout(String customerId, CheckoutRequest req) {
        // Load product + validate
        Product product = products.findById(req.productId())
                .filter(p -> p.getStatus() == ProductStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sản phẩm không còn được bán."));
        Shop shop = product.getShop();
        if (shop.getStatus() != ShopStatus.ACTIVE)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cửa hàng hiện không hoạt động.");
        if (product.getType() != ProductType.CUSTOM && product.getStock() < req.quantity())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sản phẩm không đủ số lượng. Hiện còn " + product.getStock() + " sản phẩm.");

        Address customerAddr = destination(customerId, req.addressId());

        // Calculate fees
        BigDecimal subTotal = product.getPrice().multiply(BigDecimal.valueOf(req.quantity()));
        BigDecimal shippingFee = quote(shop, customerAddr);
        BigDecimal totalAmount = subTotal.add(shippingFee);

        // Lock product and decrement stock (chỉ áp dụng với hoa có sẵn)
        Product locked = products.findByIdForUpdate(product.getId()).orElseThrow(CustomerOrderService::notFound);
        if (locked.getType() != ProductType.CUSTOM) {
            if (locked.getStock() < req.quantity())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sản phẩm vừa hết hàng. Vui lòng thử lại.");
            locked.setStock(locked.getStock() - req.quantity());
        }

        // Build entities
        String orderId = UUID.randomUUID().toString();
        User customer = users.findById(customerId).orElseThrow(CustomerOrderService::notFound);
        Order order = new Order();
        order.setId(orderId);
        order.setCustomer(customer);
        order.setShop(shop);
        order.setDeliveryAddress(customerAddr);
        order.setOrderType(OrderType.STANDARD);
        order.setSubTotal(subTotal);
        order.setShippingFee(shippingFee);
        order.setTotalAmount(totalAmount);
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedBy(customerId);
        order.setLastModifyBy(customerId);

        OrderDetail detail = OrderDetail.builder().id(UUID.randomUUID().toString()).order(order)
                .product(product).price(product.getPrice()).quantity(req.quantity()).createdBy(customerId).build();

        Payment payment = Payment.builder().id(UUID.randomUUID().toString()).order(order)
                .paymentType(PaymentType.FULL).paymentMethod(PaymentMethod.COD).amount(totalAmount)
                .status(PaymentStatus.PENDING).createdBy(customerId).build();

        orders.save(order);
        orderDetails.save(detail);
        payments.save(payment);
        return new CheckoutResponse(orderId, subTotal, shippingFee, totalAmount, OrderStatus.PENDING.name());
    }

    public Results list(String customerId, int page, String status) {
        Pageable pageable = PageRequest.of(page, 20, Sort.by(Sort.Order.desc("createdDate"), Sort.Order.asc("id")));
        Page<Order> result;
        if (status == null || status.isBlank()) {
            result = orders.findByCustomerIdOrderByCreatedDateDesc(customerId, pageable);
        } else {
            OrderStatus parsed;
            try { parsed = OrderStatus.valueOf(status.toUpperCase()); }
            catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trạng thái không hợp lệ: " + status);
            }
            result = orders.findByCustomerIdAndStatus(customerId, parsed, pageable);
        }
        var content = result.getContent().stream().map(o -> {
            long itemCount = orderDetails.findByOrderId(o.getId()).size();
            return new OrderSummary(o.getId(), o.getShop().getName(), o.getTotalAmount(),
                    o.getStatus().name(), o.getCreatedDate(), itemCount);
        }).toList();
        return new Results(content, page, result.getTotalPages(), result.getTotalElements());
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
        return new OrderDetailResponse(order.getId(), order.getShop().getId(), order.getShop().getName(),
                order.getStatus().name(), order.getSubTotal(), order.getShippingFee(), order.getTotalAmount(),
                order.getCreatedDate(), null, items, delivery);
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