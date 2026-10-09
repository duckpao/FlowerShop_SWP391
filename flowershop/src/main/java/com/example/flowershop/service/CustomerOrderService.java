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
            @Size(max=500) String note,
            Integer ghnDistrictId,
            String ghnWardCode,
            String city,
            String district,
            String ward) {}

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

    public record DeliveryInfo(String trackingCode, String deliveryPartnerId, String status) {}

    public record Results(List<OrderSummary> content, int page, int totalPages, long totalElements) {}

    private final ProductRepository products;
    private final AddressRepository addresses;
    private final OrderRepository orders;
    private final OrderDetailRepository orderDetails;
    private final PaymentRepository payments;
    private final DeliveryRepository deliveries;
    private final UserRepository users;
    private final GhnService ghn;

    public CustomerOrderService(ProductRepository products, AddressRepository addresses,
                                OrderRepository orders, OrderDetailRepository orderDetails,
                                PaymentRepository payments, DeliveryRepository deliveries,
                                UserRepository users, GhnService ghn) {
        this.products = products;
        this.addresses = addresses;
        this.orders = orders;
        this.orderDetails = orderDetails;
        this.payments = payments;
        this.deliveries = deliveries;
        this.users = users;
        this.ghn = ghn;
    }
private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy dữ liệu.");
    }

    @Transactional
    public BigDecimal calculateFee(String customerId, CheckoutRequest req) {
        Product product = products.findById(req.productId())
                .filter(p -> p.getStatus() == ProductStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sản phẩm không bán nữa."));
        Shop shop = product.getShop();
        List<Address> shopAddrs = addresses.findByShopIdAndUserIsNullOrderByCreatedDateAscIdAsc(shop.getId());
        Address shopAddr = shopAddrs.stream().filter(a -> Boolean.TRUE.equals(a.getIsDefault())).findFirst()
                .orElse(shopAddrs.isEmpty() ? null : shopAddrs.get(0));
        if (shopAddr == null || shopAddr.getGhnDistrictId() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Shop thiếu địa chỉ GHN.");
        
        Address customerAddr = addresses.findById(req.addressId())
                .filter(a -> a.getUser() != null && a.getUser().getId().equals(customerId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không tìm thấy địa chỉ."));
                
        int toDistrict = req.ghnDistrictId() != null ? req.ghnDistrictId() : customerAddr.getGhnDistrictId() != null ? customerAddr.getGhnDistrictId() : 0;
        String toWard = req.ghnWardCode() != null && !req.ghnWardCode().isBlank() ? req.ghnWardCode() : customerAddr.getGhnWardCode();
        if (toDistrict == 0 || toWard == null || toWard.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn địa chỉ GHN.");
            
        return ghn.calculateFee(shopAddr.getGhnDistrictId(), toDistrict, toWard, GhnService.DEFAULT_WEIGHT_GRAM);
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
        if (product.getStock() < req.quantity())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sản phẩm không đủ số lượng. Hiện còn " + product.getStock() + " sản phẩm.");

        // Load customer address + validate GHN codes
        Address customerAddr = addresses.findById(req.addressId())
                .filter(a -> a.getUser() != null && a.getUser().getId().equals(customerId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Địa chỉ không tồn tại hoặc không thuộc về bạn."));
        if (req.ghnDistrictId() != null && req.ghnWardCode() != null && !req.ghnWardCode().isBlank()) {
            customerAddr.setGhnDistrictId(req.ghnDistrictId());
            customerAddr.setGhnWardCode(req.ghnWardCode());
            if (req.city() != null) customerAddr.setCity(req.city());
            if (req.district() != null) customerAddr.setDistrict(req.district());
            if (req.ward() != null) customerAddr.setWard(req.ward());
            addresses.save(customerAddr);
        } else if (customerAddr.getGhnDistrictId() == null || customerAddr.getGhnWardCode() == null || customerAddr.getGhnWardCode().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Địa chỉ chưa có mã GHN, vui lòng chọn lại Tỉnh/Huyện/Xã.");
        }

        // Load shop default address + validate GHN codes
        var shopAddrs = addresses.findByShopIdAndUserIsNullOrderByCreatedDateAscIdAsc(shop.getId());
        Address shopAddr = shopAddrs.stream().filter(a -> Boolean.TRUE.equals(a.getIsDefault())).findFirst()
                .orElse(shopAddrs.isEmpty() ? null : shopAddrs.get(0));
        if (shopAddr == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cửa hàng chưa thiết lập địa chỉ giao hàng.");
        if (shopAddr.getGhnDistrictId() == null || shopAddr.getGhnWardCode() == null || shopAddr.getGhnWardCode().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cửa hàng chưa có mã GHN, vui lòng chọn lại Tỉnh/Huyện/Xã cho địa chỉ shop.");

        // Calculate fees
        BigDecimal subTotal = product.getPrice().multiply(BigDecimal.valueOf(req.quantity()));
        BigDecimal shippingFee = ghn.calculateFee(shopAddr.getGhnDistrictId(), customerAddr.getGhnDistrictId(),
                customerAddr.getGhnWardCode(), GhnService.DEFAULT_WEIGHT_GRAM);
        BigDecimal totalAmount = subTotal.add(shippingFee);

        // Lock product and decrement stock
        Product locked = products.findByIdForUpdate(product.getId()).orElseThrow(CustomerOrderService::notFound);
        if (locked.getStock() < req.quantity())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sản phẩm vừa hết hàng. Vui lòng thử lại.");
        locked.setStock(locked.getStock() - req.quantity());

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

    public Results list(String customerId, int page) {
        Pageable pageable = PageRequest.of(page, 20, Sort.by(Sort.Order.desc("createdDate"), Sort.Order.asc("id")));
        Page<Order> result = orders.findByCustomerIdOrderByCreatedDateDesc(customerId, pageable);
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
                .map(d -> new DeliveryInfo(d.getTrackingCode(), d.getDeliveryPartnerId(), d.getStatus().name()))
                .orElse(null);
        return new OrderDetailResponse(order.getId(), order.getShop().getId(), order.getShop().getName(),
                order.getStatus().name(), order.getSubTotal(), order.getShippingFee(), order.getTotalAmount(),
                order.getCreatedDate(), null, items, delivery);
    }

        public BigDecimal calculateShippingFeeForOrder(String customerId, String orderId) {
                Order order = orders.findById(orderId)
                                .filter(o -> o.getCustomer().getId().equals(customerId))
                                .orElseThrow(CustomerOrderService::notFound);
                List<Address> shopAddresses = addresses.findByShopIdAndUserIsNullOrderByCreatedDateAscIdAsc(order.getShop().getId());
                Address shopAddress = shopAddresses.stream()
                                .filter(address -> Boolean.TRUE.equals(address.getIsDefault()))
                                .findFirst()
                                .orElse(shopAddresses.isEmpty() ? null : shopAddresses.get(0));
                Address destination = order.getDeliveryAddress();

                if (shopAddress == null || shopAddress.getGhnDistrictId() == null) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Shop chưa có địa chỉ GHN hợp lệ.");
                }
                if (destination == null || destination.getGhnDistrictId() == null
                                || destination.getGhnWardCode() == null || destination.getGhnWardCode().isBlank()) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Địa chỉ nhận hàng chưa có thông tin GHN hợp lệ.");
                }

                return ghn.calculateFee(shopAddress.getGhnDistrictId(), destination.getGhnDistrictId(),
                                destination.getGhnWardCode(), GhnService.DEFAULT_WEIGHT_GRAM);
        }

        @Transactional
        public DeliveryInfo refreshTracking(String customerId, String orderId) {
                Order order = orders.findById(orderId)
                                .filter(o -> o.getCustomer().getId().equals(customerId))
                                .orElseThrow(CustomerOrderService::notFound);
                if (order.getStatus() != OrderStatus.DELIVERING && order.getStatus() != OrderStatus.COMPLETED) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "Đơn hàng chưa được bàn giao cho đơn vị vận chuyển.");
                }

                Delivery delivery = deliveries.findByOrderId(orderId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Đơn hàng chưa có vận đơn."));
                DeliveryStatus status = ghn.checkStatus(delivery.getTrackingCode());
                delivery.setStatus(status);
                delivery.setLastModifyBy(customerId);

                if (status == DeliveryStatus.DELIVERED) {
                        order.setStatus(OrderStatus.COMPLETED);
                        order.setLastModifyBy(customerId);
                        payments.findByOrderId(orderId).forEach(payment -> {
                                payment.setStatus(PaymentStatus.SUCCESS);
                                payment.setLastModifyBy(customerId);
                        });
                }

                return new DeliveryInfo(delivery.getTrackingCode(), delivery.getDeliveryPartnerId(), status.name());
        }
}