package com.example.flowershop.service;

import com.example.flowershop.dto.order.MakeOrderRequest;
import com.example.flowershop.dto.order.OrderResponse;
import com.example.flowershop.entity.*;
import com.example.flowershop.entity.enums.OrderStatus;
import com.example.flowershop.entity.enums.OrderType;
import com.example.flowershop.entity.enums.PaymentMethod;
import com.example.flowershop.entity.enums.PaymentStatus;
import com.example.flowershop.entity.enums.PaymentType;
import com.example.flowershop.entity.enums.ProductStatus;
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
import java.util.Optional;
import java.util.stream.Collectors;

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
    private final DeliveryAreaService deliveryAreaService;

    /**
     * Tạo đơn hàng từ các cart items đã chọn.
     * Flow:
     *  1. Validate user, address, cart items
     *  2. Tính toán subTotal, totalAmount
     *  3. Tạo Order
     *  4. Tạo OrderDetail cho mỗi sản phẩm
     *  5. Giảm stock sản phẩm
     *  6. Xóa các cart items đã chọn
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
        if (request.getRecipientName() == null || request.getRecipientName().isBlank()
                || request.getPhone() == null || request.getPhone().isBlank()) {
            throw ApiException.badRequest("Cần nhập tên và số điện thoại người nhận.");
        }

        // 2. Validate delivery address (tạm thời bỏ qua check nghiêm ngặt theo yêu cầu)
        String rawAddressInput = (request.getDeliveryAddressId() != null && !request.getDeliveryAddressId().isBlank())
                ? request.getDeliveryAddressId().trim()
                : "Hà Nội";
        final String addressValue = rawAddressInput;

        Address deliveryAddress = addressRepository.findById(addressValue)
                .filter(addr -> addr.getUser() != null && addr.getUser().getId().equals(userId))
                .orElseGet(() -> {
                    // Ưu tiên địa chỉ mặc định của user nếu có
                    Optional<Address> defaultAddress = addressRepository.findByUserIdAndIsDefaultTrue(userId);
                    if (defaultAddress.isPresent()) {
                        return defaultAddress.get();
                    }
                    List<Address> userAddresses = addressRepository.findByUserId(userId);
                    if (!userAddresses.isEmpty()) {
                        return userAddresses.get(0);
                    }
                    // Tạo mới address linh hoạt chấp nhận mọi địa chỉ người dùng nhập
                    Address newAddress = Address.builder()
                            .id("addr-" + UUID.randomUUID().toString().substring(0, 8))
                            .user(user)
                            .addressLine(addressValue.length() > 255 ? addressValue.substring(0, 255) : addressValue)
                            .city("Hà Nội")
                            .isDefault(false)
                            .createdBy(userId)
                            .build();
                    return addressRepository.save(newAddress);
                });

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
        selectedCartItems.sort(Comparator.comparing(item -> item.getProduct().getId()));
        for (CartItem cartItem : selectedCartItems) {
            Product product = productRepository.findByIdForUpdate(cartItem.getProduct().getId())
                    .orElseThrow(() -> ApiException.notFound("Sản phẩm không còn tồn tại."));
            cartItem.setProduct(product);
            if (product.getStatus() != ProductStatus.ACTIVE || product.getShop().getStatus() != ShopStatus.ACTIVE) {
                throw ApiException.badRequest("Sản phẩm hoặc cửa hàng không còn hoạt động.");
            }
            if (product.getStock() < cartItem.getQuantity()) {
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
        BigDecimal totalAmount = subTotal.subtract(discountAmount);
        if (totalAmount.signum() <= 0) throw ApiException.badRequest("Tổng thanh toán phải lớn hơn 0.");

        // 5. Tạo Order
        Shop shop = selectedCartItems.get(0).getProduct().getShop();
        Order order = Order.builder()
                .id(UUID.randomUUID().toString())
                .customer(user)
                .shop(shop)
                .deliveryAddress(deliveryAddress)
                .recipientName(request.getRecipientName().trim())
                .recipientPhone(request.getPhone().trim())
                .couponId(couponId)
                .orderType(OrderType.STANDARD)
                .subTotal(subTotal)
                .discountAmount(discountAmount)
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

            // Giảm stock
            product.setStock(product.getStock() - cartItem.getQuantity());
            product.setLastModifyBy(userId);
            productRepository.save(product);

            orderItemResponses.add(OrderResponse.OrderItemResponse.builder()
                    .id(orderDetail.getId())
                    .productId(product.getId())
                    .productName(product.getName())
                    .price(product.getPrice())
                    .quantity(cartItem.getQuantity())
                    .itemTotal(itemTotal)
                    .build());
        }

        // 7. Xóa cart items đã đặt hàng
        cartItemRepository.deleteAll(selectedCartItems);

        // 8. Xử lý phương thức thanh toán
        boolean isCod = "COD".equalsIgnoreCase(request.getPaymentMethod());
        if (isCod) {
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

        return OrderResponse.builder()
                .id(order.getId())
                .status(order.getStatus().name())
                .orderType(order.getOrderType().name())
                .subTotal(subTotal)
                .discountAmount(discountAmount)
                .totalAmount(totalAmount)
                .deliveryAddressId(deliveryAddress.getId())
                .recipientName(order.getRecipientName())
                .recipientPhone(order.getRecipientPhone())
                .items(orderItemResponses)
                .createdDate(order.getCreatedDate())
                .build();
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
                    .totalAmount(order.getTotalAmount())
                    .deliveryAddressId(order.getDeliveryAddress().getId())
                    .recipientName(order.getRecipientName())
                    .recipientPhone(order.getRecipientPhone())
                    .items(items)
                    .createdDate(order.getCreatedDate())
                    .build();
        }).collect(Collectors.toList());
    }
}

