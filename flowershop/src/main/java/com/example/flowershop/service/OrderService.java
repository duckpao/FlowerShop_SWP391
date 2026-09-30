package com.example.flowershop.service;

import com.example.flowershop.dto.order.MakeOrderRequest;
import com.example.flowershop.dto.order.OrderResponse;
import com.example.flowershop.entity.*;
import com.example.flowershop.entity.enums.OrderStatus;
import com.example.flowershop.entity.enums.OrderType;
import com.example.flowershop.entity.enums.PaymentMethod;
import com.example.flowershop.entity.enums.PaymentStatus;
import com.example.flowershop.entity.enums.PaymentType;
import com.example.flowershop.exception.ApiException;
import com.example.flowershop.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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

        boolean userUpdated = false;
        if (request.getPhone() != null && !request.getPhone().trim().isEmpty() && (user.getPhone() == null || user.getPhone().trim().isEmpty())) {
            user.setPhone(request.getPhone().trim());
            userUpdated = true;
        }
        if (request.getRecipientName() != null && !request.getRecipientName().trim().isEmpty() && (user.getFullName() == null || user.getFullName().trim().isEmpty())) {
            user.setFullName(request.getRecipientName().trim());
            userUpdated = true;
        }
        if (userUpdated) {
            userRepository.save(user);
        }

        // 2. Validate delivery address (hỗ trợ cả Address ID lẫn nhập địa chỉ trực tiếp)
        String rawAddressInput = (request.getDeliveryAddressId() != null) ? request.getDeliveryAddressId().trim() : "";
        if (rawAddressInput.isEmpty()) {
            rawAddressInput = "Hà Nội";
        }
        final String addressValue = rawAddressInput;

        Address deliveryAddress = addressRepository.findById(addressValue)
                .filter(addr -> addr.getUser() != null && addr.getUser().getId().equals(userId))
                .orElseGet(() -> {
                    // Nếu người dùng nhập trực tiếp địa chỉ dạng text (ví dụ: "ha noi", "Số 10 Cầu Giấy, Hà Nội")
                    // Hoặc ID chưa có trong DB, tự động tạo và lưu Address cho user này:
                    Address newAddress = Address.builder()
                            .id("addr-" + UUID.randomUUID().toString().substring(0, 8))
                            .user(user)
                            .addressLine(addressValue)
                            .city("Hà Nội")
                            .isDefault(false)
                            .createdBy(userId)
                            .build();
                    return addressRepository.save(newAddress);
                });

        // 3. Validate shop
        // Lấy danh sách cart items được chọn
        List<CartItem> selectedCartItems = new ArrayList<>();
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

            // Kiểm tra stock tại thời điểm đặt hàng
            Product product = cartItem.getProduct();
            if (product.getStock() < cartItem.getQuantity()) {
                throw ApiException.badRequest(
                        "Sản phẩm '" + product.getName() + "' không đủ tồn kho. " +
                        "Tồn kho: " + product.getStock() + ", Số lượng đặt: " + cartItem.getQuantity()
                );
            }

            selectedCartItems.add(cartItem);
        }

        // 4. Tính toán tổng tiền
        BigDecimal subTotal = selectedCartItems.stream()
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // TODO: Tích hợp Coupon discount ở bước sau
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal totalAmount = subTotal.subtract(discountAmount);

        // 5. Tạo Order
        Shop shop = selectedCartItems.get(0).getProduct().getShop();
        Order order = Order.builder()
                .id(UUID.randomUUID().toString())
                .customer(user)
                .shop(shop)
                .deliveryAddress(deliveryAddress)
                .couponId(request.getCouponId())
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
                    .items(items)
                    .createdDate(order.getCreatedDate())
                    .build();
        }).collect(Collectors.toList());
    }
}

