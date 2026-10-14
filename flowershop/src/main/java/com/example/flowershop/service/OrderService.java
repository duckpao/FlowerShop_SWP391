package com.example.flowershop.service;

import com.example.flowershop.dto.order.MakeOrderRequest;
import com.example.flowershop.dto.order.MakeOrdersRequest;
import com.example.flowershop.dto.order.ShippingFeeRequest;
import com.example.flowershop.dto.order.OrderResponse;
import com.example.flowershop.entity.*;
import com.example.flowershop.entity.enums.OrderStatus;
import com.example.flowershop.entity.enums.OrderType;
import com.example.flowershop.entity.enums.PaymentMethod;
import com.example.flowershop.entity.enums.PaymentStatus;
import com.example.flowershop.entity.enums.PaymentType;
import com.example.flowershop.entity.enums.ProductStatus;
import com.example.flowershop.entity.enums.ProductType;
import com.example.flowershop.entity.enums.ShopStatus;
import com.example.flowershop.entity.enums.CouponStatus;
import com.example.flowershop.entity.enums.DiscountType;
import com.example.flowershop.exception.ApiException;
import com.example.flowershop.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Comparator;
import java.util.HashSet;
import java.time.LocalDateTime;
import java.math.RoundingMode;
import lombok.extern.slf4j.Slf4j;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final ProductRepository productRepository;
    private final PaymentRepository paymentRepository;
    private final CouponRepository couponRepository;
    private final ShopRepository shopRepository;
    private final GhnService ghn;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;

    /** Ước tính phí giao hàng cho trang checkout. */
    @Transactional(readOnly = true)
    public BigDecimal estimateShippingFee(String userId, String shopId, String deliveryAddressId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy cửa hàng."));
        return quoteFee(shop, resolveDestination(userId, deliveryAddressId));
    }

    /**
     * Đặt nhiều đơn một lúc (mỗi shop một đơn). Cùng một transaction: một shop lỗi thì cả nhóm rollback.
     * Sắp xếp theo shopId để thứ tự khóa kho ổn định giữa các request đồng thời.
     */
    @Transactional
    public List<OrderResponse> makeOrders(String userId, MakeOrdersRequest request) {
        var parts = request.getShops().stream()
                .sorted(Comparator.comparing(MakeOrdersRequest.ShopOrder::getShopId)).toList();
        if (parts.stream().map(MakeOrdersRequest.ShopOrder::getShopId).distinct().count() != parts.size()) {
            throw ApiException.badRequest("Mỗi shop chỉ được xuất hiện một lần trong yêu cầu đặt hàng.");
        }
        // Nhiều shop: gắn chung một mã nhóm để màn đơn hàng hiển thị và thanh toán như một đơn.
        String group = parts.size() > 1 ? UUID.randomUUID().toString() : null;
        List<OrderResponse> orders = new ArrayList<>();
        for (var part : parts) {
            MakeOrderRequest single = new MakeOrderRequest();
            single.setShopId(part.getShopId());
            single.setCartItemIds(part.getCartItemIds());
            single.setCouponId(part.getCouponId());
            single.setDeliveryAddressId(request.getDeliveryAddressId());
            single.setPaymentMethod(request.getPaymentMethod());
            single.setRecipientName(request.getRecipientName());
            single.setPhone(request.getPhone());
            OrderResponse created = makeOrder(userId, single);
            if (group != null) {
                Order saved = orderRepository.findById(created.getId()).orElseThrow();
                saved.setCheckoutGroup(group);
                orderRepository.save(saved);
            }
            orders.add(created);
        }
        return orders;
    }

    /**
     * Tạo đơn hàng từ các cart items đã chọn.
     * Flow:
     *  1. Validate user, address, cart items
     *  2. Tính toán subTotal, totalAmount
     *  3. Tạo Order
     *  4. Tạo OrderDetail cho mỗi sản phẩm
     *  5. Giảm stock sản phẩm
     *  6. Giữ cart items của đơn ONLINE cho tới khi thanh toán được xác nhận
     */
    @Transactional
    public OrderResponse makeOrder(String userId, MakeOrderRequest request) {
        // 1. Validate user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy user với id: " + userId));

        if (!"COD".equalsIgnoreCase(request.getPaymentMethod())
                && !"ONLINE".equalsIgnoreCase(request.getPaymentMethod())) {
            throw ApiException.badRequest("Phương thức thanh toán không hợp lệ.");
        }
        String recipientName = firstNonBlank(request.getRecipientName(), user.getFullName());
        String recipientPhone = firstNonBlank(request.getPhone(), user.getPhone());
        if (recipientName == null || recipientPhone == null) {
            throw ApiException.badRequest("Cần có tên và số điện thoại người nhận. Hãy cập nhật số điện thoại trong tài khoản.");
        }

        // 2. Địa chỉ nhận phải là địa chỉ đã lưu của khách và có mã GHN để tính phí
        Address deliveryAddress = resolveDestination(userId, request.getDeliveryAddressId());

        // 3. Validate shop
        // Lấy danh sách cart items được chọn
        List<CartItem> selectedCartItems = new ArrayList<>();
        if (new HashSet<>(request.getCartItemIds()).size() != request.getCartItemIds().size()) {
            throw ApiException.badRequest("Danh sách sản phẩm bị trùng.");
        }
        for (String cartItemId : request.getCartItemIds()) {
            CartItem cartItem = cartItemRepository.findById(cartItemId)
                    .orElseThrow(() -> ApiException.notFound("Cart item không tồn tại: " + cartItemId));

            // Cart item phải thuộc user này
            if (!cartItem.getUser().getId().equals(userId)) {
                throw ApiException.forbidden("Cart item không thuộc về bạn: " + cartItemId);
            }

            // Cart item phải thuộc đúng shop được chọn
            if (!cartItem.getProduct().getShop().getId().equals(request.getShopId())) {
                throw ApiException.badRequest(
                        "Sản phẩm '" + cartItem.getProduct().getName() + "' không thuộc shop đã chọn. " +
                        "Mỗi đơn hàng chỉ được đặt từ 1 shop."
                );
            }

            selectedCartItems.add(cartItem);
        }
        // Đặt lại món đang nằm trong đơn PENDING cũ của chính khách: hủy đơn cũ (trả kho) thay vì tạo đơn trùng trừ kho lần nữa.
        // Không khôi phục giỏ ở đây, vì giỏ hiện tại (số lượng khách vừa chỉnh) mới là thứ khách muốn đặt.
        for (String pendingId : orderDetailRepository.findPendingOrderIds(userId,
                selectedCartItems.stream().map(item -> item.getProduct().getId()).toList())) {
            cancelPending(pendingId, "Khách đặt lại đơn mới cho cùng sản phẩm", userId, false);
        }
        selectedCartItems.sort(Comparator.comparing(item -> item.getProduct().getId()));
        for (CartItem cartItem : selectedCartItems) {
            Product product = productRepository.findByIdForUpdate(cartItem.getProduct().getId())
                    .orElseThrow(() -> ApiException.notFound("Sản phẩm không còn tồn tại."));
            cartItem.setProduct(product);
            if (product.getStatus() != ProductStatus.ACTIVE || product.getShop().getStatus() != ShopStatus.ACTIVE) {
                throw ApiException.badRequest("Sản phẩm hoặc cửa hàng không còn hoạt động.");
            }
            if (product.getType() != ProductType.CUSTOM && product.getStock() < cartItem.getQuantity()) {
                throw ApiException.badRequest(
                        "Sản phẩm '" + product.getName() + "' không đủ tồn kho. " +
                        "Tồn kho: " + product.getStock() + ", Số lượng đặt: " + cartItem.getQuantity()
                );
            }

        }

        // 4. Tính toán tổng tiền
        BigDecimal subTotal = selectedCartItems.stream()
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discountAmount = BigDecimal.ZERO;
        String couponId = null;
        if (request.getCouponId() != null && !request.getCouponId().isBlank()) {
            Coupon coupon = couponRepository.findByCodeForUpdate(request.getCouponId().trim())
                    .orElseThrow(() -> ApiException.badRequest("Mã giảm giá không hợp lệ."));
            LocalDateTime now = LocalDateTime.now();
            if (coupon.getStatus() != CouponStatus.ACTIVE || now.isBefore(coupon.getStartDate())
                    || now.isAfter(coupon.getEndDate()) || coupon.getUsedCount() >= coupon.getUsageLimit()
                    || coupon.getDiscountValue().signum() <= 0
                    || (coupon.getDiscountType() == DiscountType.PERCENTAGE && coupon.getDiscountValue().compareTo(new BigDecimal("100")) > 0)
                    || subTotal.compareTo(coupon.getMinOrderValue()) < 0
                    || (coupon.getShop() != null && !coupon.getShop().getId().equals(request.getShopId()))) {
                throw ApiException.badRequest("Mã giảm giá không áp dụng được cho đơn hàng này.");
            }
            discountAmount = coupon.getDiscountType() == DiscountType.PERCENTAGE
                    ? subTotal.multiply(coupon.getDiscountValue()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
                    : coupon.getDiscountValue();
            if (coupon.getMaxDiscountValue() != null) discountAmount = discountAmount.min(coupon.getMaxDiscountValue());
            discountAmount = discountAmount.min(subTotal);
            if (discountAmount.signum() < 0) throw ApiException.badRequest("Mã giảm giá không hợp lệ.");
            coupon.setUsedCount(coupon.getUsedCount() + 1);
            couponRepository.save(coupon);
            couponId = coupon.getId();
        }
        if (subTotal.subtract(discountAmount).signum() <= 0) throw ApiException.badRequest("Tổng thanh toán phải lớn hơn 0.");

        // Phí vận chuyển do GHN tính; khách trả tiền hàng + phí này
        Shop shop = selectedCartItems.get(0).getProduct().getShop();
        BigDecimal shippingFee = quoteFee(shop, deliveryAddress);
        BigDecimal totalAmount = subTotal.subtract(discountAmount).add(shippingFee);

        // 5. Tạo Order
        Order order = Order.builder()
                .id(UUID.randomUUID().toString())
                .customer(user)
                .shop(shop)
                .deliveryAddress(deliveryAddress)
                .recipientName(recipientName)
                .recipientPhone(recipientPhone)
                .couponId(couponId)
                .orderType(OrderType.STANDARD)
                .subTotal(subTotal)
                .discountAmount(discountAmount)
                .shippingFee(shippingFee)
                .totalAmount(totalAmount)
                .depositAmount(BigDecimal.ZERO)
                .status(OrderStatus.PENDING)
                .createdBy(userId)
                .lastModifyBy(userId)
                .build();

        order = orderRepository.save(order);

        // 6. Tạo OrderDetail và giảm stock từng sản phẩm
        List<OrderResponse.OrderItemResponse> orderItemResponses = new ArrayList<>();

        for (CartItem cartItem : selectedCartItems) {
            Product product = cartItem.getProduct();
            BigDecimal itemTotal = product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));

            // Tạo OrderDetail
            OrderDetail orderDetail = OrderDetail.builder()
                    .id(UUID.randomUUID().toString())
                    .order(order)
                    .product(product)
                    .price(product.getPrice())
                    .quantity(cartItem.getQuantity())
                    .createdBy(userId)
                    .lastModifyBy(userId)
                    .build();

            orderDetailRepository.save(orderDetail);

            // Giảm stock (chỉ áp dụng với hoa có sẵn)
            if (product.getType() != ProductType.CUSTOM) {
                product.setStock(product.getStock() - cartItem.getQuantity());
                product.setLastModifyBy(userId);
                productRepository.save(product);
            }

            orderItemResponses.add(OrderResponse.OrderItemResponse.builder()
                    .id(orderDetail.getId())
                    .productId(product.getId())
                    .productName(product.getName())
                    .price(product.getPrice())
                    .quantity(cartItem.getQuantity())
                    .itemTotal(itemTotal)
                    .build());
        }

        // 7. Xử lý phương thức thanh toán
        boolean isCod = "COD".equalsIgnoreCase(request.getPaymentMethod());
        if (isCod) {
            // COD được xác nhận ngay khi tạo đơn; đơn ONLINE chỉ xóa giỏ khi ngân hàng xác nhận.
            cartItemRepository.deleteAll(selectedCartItems);
            // COD: tạo bản ghi thanh toán ngay, trạng thái PENDING (sẽ thu tiền khi giao hàng)
            Payment codPayment = Payment.builder()
                    .id(UUID.randomUUID().toString())
                    .order(order)
                    .paymentType(PaymentType.FULL)
                    .paymentMethod(PaymentMethod.COD)
                    .amount(totalAmount)
                    .invoiceNumber("COD-" + order.getId().replace("-", "").substring(0, 8).toUpperCase())
                    .status(PaymentStatus.PENDING)
                    .createdBy(userId)
                    .lastModifyBy(userId)
                    .build();
            paymentRepository.save(codPayment);

            // Đơn COD chuyển sang PROCESSING ngay (đã xác nhận, chờ giao hàng)
            order.setStatus(OrderStatus.PROCESSING);
            order.setLastModifyBy(userId);
            order = orderRepository.save(order);
        }

        entityManager.flush(); // @CreationTimestamp chỉ được gán khi flush; thiếu dòng này response trả createdDate=null
        return OrderResponse.builder()
                .id(order.getId())
                .status(order.getStatus().name())
                .orderType(order.getOrderType().name())
                .subTotal(subTotal)
                .discountAmount(discountAmount)
                .shippingFee(shippingFee)
                .totalAmount(totalAmount)
                .deliveryAddressId(deliveryAddress.getId())
                .recipientName(order.getRecipientName())
                .recipientPhone(order.getRecipientPhone())
                .items(orderItemResponses)
                .createdDate(order.getCreatedDate())
                .build();
    }

    /** Xóa đúng số lượng của đơn đã thanh toán, giữ lại món khác hoặc số lượng khách thêm sau đó. */
    @Transactional
    public void clearPaidOrderItems(Order order) {
        if (order == null || order.getCustomer() == null) return;
        String userId = order.getCustomer().getId();
        for (OrderDetail detail : orderDetailRepository.findByOrderId(order.getId())) {
            if (detail.getProduct() == null) continue;
            cartItemRepository.findByUserIdAndProductIdForUpdate(userId, detail.getProduct().getId())
                    .ifPresent(cartItem -> {
                        int remaining = cartItem.getQuantity() - detail.getQuantity();
                        if (remaining <= 0) cartItemRepository.delete(cartItem);
                        else {
                            cartItem.setQuantity(remaining);
                            cartItemRepository.save(cartItem);
                        }
                    });
        }
    }

    /** Khôi phục giỏ từ chi tiết đơn bị hủy, kể cả đơn cũ đã xóa giỏ lúc tạo đơn. */
    @Transactional
    public void restoreCancelledOrderItems(Order order) {
        if (order == null || order.getStatus() != OrderStatus.CANCELLED || order.getCustomer() == null) return;
        String userId = order.getCustomer().getId();
        for (OrderDetail detail : orderDetailRepository.findByOrderId(order.getId())) {
            if (detail.getProduct() == null) continue;
            String productId = detail.getProduct().getId();
            CartItem current = cartItemRepository.findByUserIdAndProductIdForUpdate(userId, productId).orElse(null);
            if (current == null) {
                cartItemRepository.save(CartItem.builder()
                        .id(UUID.randomUUID().toString())
                        .user(order.getCustomer())
                        .product(detail.getProduct())
                        .quantity(detail.getQuantity())
                        .createdBy(userId)
                        .lastModifyBy(userId)
                        .build());
            } else if (current.getQuantity() < detail.getQuantity()) {
                current.setQuantity(detail.getQuantity());
                current.setLastModifyBy(userId);
                cartItemRepository.save(current);
            }
        }
    }

    /** Phí GHN cho một đơn giao tới địa chỉ đã lưu của khách, để hiển thị trước khi đặt. */
    @Transactional
    public BigDecimal quoteShippingFee(String userId, ShippingFeeRequest request) {
        Shop shop = shopRepository.findById(request.getShopId())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy cửa hàng."));
        return quoteFee(shop, resolveDestination(userId, request.getDeliveryAddressId()));
    }

    /** Fee from the shop's pickup address (default or first, with GHN codes) to the customer's address. */
    private BigDecimal quoteFee(Shop shop, Address destination) {
        List<Address> list = addressRepository.findByShopIdAndUserIsNullOrderByCreatedDateAscIdAsc(shop.getId());
        Address from = list.stream().filter(a -> Boolean.TRUE.equals(a.getIsDefault())).findFirst()
                .orElse(list.isEmpty() ? null : list.get(0));
        if (from == null || from.getGhnDistrictId() == null || from.getGhnWardCode() == null)
            throw ApiException.badRequest("Cửa hàng chưa có địa chỉ lấy hàng theo danh mục GHN nên chưa tính được phí vận chuyển.");
        return ghn.calculateFee(from.getGhnDistrictId(), from.getGhnWardCode(),
                destination.getGhnDistrictId(), destination.getGhnWardCode(), GhnService.DEFAULT_WEIGHT_GRAM);
    }

    /** The customer's saved address; it must carry the GHN district id and ward code a fee quote needs. */
    private Address resolveDestination(String userId, String addressId) {
        Address address = addressRepository.findById(addressId == null ? "" : addressId.trim())
                .filter(a -> a.getUser() != null && a.getUser().getId().equals(userId))
                .orElseThrow(() -> ApiException.badRequest("Vui lòng chọn một địa chỉ đã lưu trong tài khoản."));
        if (address.getGhnDistrictId() == null || address.getGhnWardCode() == null)
            throw ApiException.badRequest("Địa chỉ chưa có mã quận/huyện và phường/xã GHN. Hãy lưu lại địa chỉ trong hồ sơ.");
        return address;
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) return first.trim();
        if (second != null && !second.isBlank()) return second.trim();
        return null;
    }

    /**
     * Lấy lịch sử đơn hàng của user.
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrderHistory(String userId) {
        if (!userRepository.existsById(userId)) {
            throw ApiException.notFound("Không tìm thấy user với id: " + userId);
        }

        List<Order> orders = orderRepository.findByCustomerIdOrderByCreatedDateDesc(userId);

        return orders.stream().map(order -> {
            List<OrderDetail> details = orderDetailRepository.findByOrderId(order.getId());
            List<OrderResponse.OrderItemResponse> items = details.stream()
                    .map(d -> OrderResponse.OrderItemResponse.builder()
                            .id(d.getId())
                            .productId(d.getProduct() != null ? d.getProduct().getId() : null)
                            .productName(d.getProduct() != null ? d.getProduct().getName() : "Sản phẩm custom")
                            .price(d.getPrice())
                            .quantity(d.getQuantity())
                            .itemTotal(d.getPrice().multiply(BigDecimal.valueOf(d.getQuantity())))
                            .build())
                    .collect(Collectors.toList());

            return OrderResponse.builder()
                    .id(order.getId())
                    .status(order.getStatus().name())
                    .orderType(order.getOrderType().name())
                    .subTotal(order.getSubTotal())
                    .discountAmount(order.getDiscountAmount())
                    .shippingFee(order.getShippingFee())
                    .totalAmount(order.getTotalAmount())
                    .deliveryAddressId(order.getDeliveryAddress().getId())
                    .recipientName(order.getRecipientName())
                    .recipientPhone(order.getRecipientPhone())
                    .items(items)
                    .createdDate(order.getCreatedDate())
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * Tự động hủy đơn hàng quá hạn chưa thanh toán (PENDING 10 phút, AWAITING_DEPOSIT 24 giờ).
     * Hoàn lại tồn kho cho từng sản phẩm, hoàn lại coupon (nếu có), và hủy các payment pending.
     */
    @Transactional
    public void cancelExpiredOrder(String orderId, String reason) {
        cancelPendingOrder(orderId, reason, "SYSTEM_AUTO_CANCEL");
    }

    /** @return true nếu đơn thực sự bị hủy (false nếu đơn đã được thanh toán/xử lý trước đó). */
    @Transactional
    public boolean cancelPendingOrder(String orderId, String reason, String actor) {
        return cancelPending(orderId, reason, actor, true);
    }

    private boolean cancelPending(String orderId, String reason, String actor, boolean restoreCart) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) return false;
        // Khóa và đọc lại từ DB: entity trong persistence context có thể đã cũ so với IPN vừa commit.
        entityManager.refresh(order, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.AWAITING_DEPOSIT) {
            return false;
        }

        log.info("Hủy đơn hàng chờ thanh toán: #{} (Lý do: {})", order.getId(), reason);

        // 1. Cập nhật trạng thái Order sang CANCELLED
        order.setStatus(OrderStatus.CANCELLED);
        order.setLastModifyBy(actor);
        orderRepository.save(order);

        // 2. Hoàn lại stock cho các sản phẩm trong đơn
        List<OrderDetail> details = orderDetailRepository.findByOrderId(order.getId());
        for (OrderDetail item : details) {
            if (item.getProduct() != null) {
                Product product = productRepository.findByIdForUpdate(item.getProduct().getId()).orElse(null);
                if (product != null && product.getType() != ProductType.CUSTOM) {
                    product.setStock(product.getStock() + item.getQuantity());
                    product.setLastModifyBy(actor);
                    productRepository.save(product);
                    log.info("Hoàn lại stock sản phẩm {}: +{}", product.getId(), item.getQuantity());
                }
            }
        }

        // 3. Hoàn lại lượt dùng coupon nếu có
        if (order.getCouponId() != null && !order.getCouponId().isBlank()) {
            couponRepository.findById(order.getCouponId()).ifPresent(coupon -> {
                coupon.setUsedCount(Math.max(0, coupon.getUsedCount() - 1));
                couponRepository.save(coupon);
                log.info("Hoàn lại coupon: {}", coupon.getCode());
            });
        }

        // 4. Hủy các payment PENDING của đơn hàng
        paymentRepository.failPendingByOrderId(order.getId(), actor);
        if (restoreCart) restoreCancelledOrderItems(order);
        return true;
    }
}

