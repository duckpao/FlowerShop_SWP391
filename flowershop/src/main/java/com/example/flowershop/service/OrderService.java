package com.example.flowershop.service;

import com.example.flowershop.dto.order.MakeOrderRequest;
import com.example.flowershop.dto.order.ShippingFeeRequest;
import com.example.flowershop.dto.order.MakeOrdersRequest;
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
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Comparator;
import java.util.HashSet;
import java.time.LocalDateTime;
import java.math.RoundingMode;
import lombok.extern.slf4j.Slf4j;

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
    private final CouponService vouchers;
    private final ShopRepository shopRepository;
    private final GhnService ghn;
    private final NotificationService notificationService;
    private final org.springframework.context.ApplicationEventPublisher events;
    private final RefundRepository refundRepository;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;
    @org.springframework.beans.factory.annotation.Autowired private ProductQuotaService quota;

    /** Ước tính phí giao hàng cho trang checkout: tìm shop, kiểm tra địa chỉ nhận của khách rồi hỏi GHN. */
    @Transactional(readOnly = true)
    public BigDecimal estimateShippingFee(String userId, String shopId, String deliveryAddressId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy cửa hàng."));
        return quoteFee(shop, resolveDestination(userId, deliveryAddressId));
    }

    /**
     * Đặt nhiều đơn một lúc (mỗi shop một đơn). Cùng một transaction: một shop lỗi thì cả nhóm rollback.
     * Sắp xếp theo shopId để thứ tự khóa kho ổn định giữa các request đồng thời (tránh deadlock).
     * Từ chối nếu một shop xuất hiện 2 lần. Nếu có >1 shop, các đơn được gắn chung checkoutGroup
     * để thanh toán gộp. Mỗi phần được chuyển thành MakeOrderRequest rồi gọi makeOrder.
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
single.setShippingCouponCode(part.getShippingCouponCode()); single.setExpectedTotal(part.getExpectedTotal());
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
        // Một biên lai gộp cho cả lần checkout; mail gửi sau khi transaction commit
        events.publishEvent(new com.example.flowershop.event.OrderPlacedEvent(orders.stream().map(OrderResponse::getId).toList()));
        return orders;
    }

    /**
     * Create the fulfillment order only after the shop accepts and prices a separate request.
     */
    @Transactional
    public OrderResponse makeCustomOrder(String userId, String requestId, com.example.flowershop.dto.order.MakeCustomOrderRequest request) {
        PaymentType paymentType=request.getPaymentType()==null?PaymentType.DEPOSIT:request.getPaymentType();
        if(paymentType!=PaymentType.DEPOSIT && paymentType!=PaymentType.FULL)
            throw ApiException.badRequest("Chọn đặt cọc và trả COD, hoặc thanh toán toàn bộ qua SePay.");
        CustomOrderRequest custom = entityManager.find(CustomOrderRequest.class, requestId,
                jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if (custom == null || !custom.getCustomer().getId().equals(userId))
            throw ApiException.notFound("Không tìm thấy yêu cầu hoa tùy chỉnh.");
        if (custom.getStatus() != com.example.flowershop.entity.enums.CustomOrderStatus.ACCEPTED)
            throw ApiException.badRequest("Shop chưa chấp nhận yêu cầu này.");
        if (orderRepository.existsByCustomRequestIdAndStatusNot(requestId, OrderStatus.CANCELLED))
            throw ApiException.badRequest("Yêu cầu này đã có đơn đặt cọc.");
        var price = customPrice(custom);
        BigDecimal quotedPrice = price.amount();
        Integer depositPercent = price.depositPercent();

        Shop shop = custom.getShop();
        if (shop.getStatus() != ShopStatus.ACTIVE) throw ApiException.badRequest("Cửa hàng hiện không hoạt động.");
        User user = custom.getCustomer();
        Address deliveryAddress = resolveDestination(userId, request.getDeliveryAddressId());
        BigDecimal shippingFee = quoteFee(shop, deliveryAddress);
        BigDecimal totalAmount = quotedPrice.add(shippingFee);

        Order order = orderRepository.save(Order.builder()
                .id(UUID.randomUUID().toString())
                .customer(user)
                .shop(shop)
                .deliveryAddress(deliveryAddress)
                .recipientName(firstNonBlank(request.getRecipientName(), user.getFullName()))
                .recipientPhone(firstNonBlank(request.getPhone(), user.getPhone()))
                .orderType(OrderType.CUSTOM)
                .customRequestId(requestId)
                .subTotal(quotedPrice)
                .shippingFee(shippingFee)
                .totalAmount(totalAmount)
                .depositAmount(BigDecimal.ZERO)
                .customerNote(custom.getDescription())
                .status(paymentType==PaymentType.FULL?OrderStatus.PENDING:OrderStatus.AWAITING_DEPOSIT)
                .paymentStartedAt(paymentType==PaymentType.FULL?java.time.LocalDateTime.now():null)
                .createdBy(userId)
                .lastModifyBy(userId)
                .build());
        // A custom design is not the reference catalog product: only ORDER/SHIPPING vouchers apply.
        vouchers.apply(order,new CouponService.Context(shop.getId(),quotedPrice,shippingFee,java.util.Map.of()),
                request.getCouponCode(),request.getShippingCouponCode());
        if(request.getExpectedTotal()!=null && request.getExpectedTotal().compareTo(order.getTotalAmount())!=0)
            throw ApiException.badRequest("Giá hoặc voucher đã thay đổi. Vui lòng kiểm tra lại trước khi đặt cọc.");
        // New quotes: deposit is a percentage of flower price AFTER the order voucher.
        // Legacy quotes without a percentage keep the existing 50% of net total fallback.
        order.setDepositAmount(CustomOrderPricing.deposit(quotedPrice,order.getDiscountAmount(),order.getTotalAmount(),depositPercent));
        if(paymentType==PaymentType.DEPOSIT && request.getExpectedDeposit()!=null && request.getExpectedDeposit().compareTo(order.getDepositAmount())!=0)
            throw ApiException.badRequest("Mức cọc đã thay đổi. Vui lòng tải lại báo giá trước khi thanh toán.");
        // Hoa làm theo yêu cầu không có sản phẩm trong kho: một dòng giá = báo giá, kèm thiết kế khách đã gửi.
        OrderDetail detail = orderDetailRepository.save(OrderDetail.builder()
                .id(UUID.randomUUID().toString())
                .order(order)
                .customMaterials(custom.getDesiredComponents())
                .price(quotedPrice)
                .quantity(1)
                .createdBy(userId)
                .lastModifyBy(userId)
                .build());

        entityManager.flush();
        events.publishEvent(new com.example.flowershop.event.OrderPlacedEvent(List.of(order.getId())));
        return OrderResponse.builder()
                .id(order.getId())
                .status(order.getStatus().name())
                .orderType(order.getOrderType().name())
                .subTotal(quotedPrice)
                .discountAmount(order.getDiscountAmount())
                .shippingDiscountAmount(order.getShippingDiscountAmount())
                .couponCode(order.getCouponCode())
                .shippingCouponCode(order.getShippingCouponCode())
                .shippingFee(shippingFee)
                .totalAmount(order.getTotalAmount())
                .depositAmount(order.getDepositAmount())
                .deliveryAddressId(deliveryAddress.getId())
                .recipientName(order.getRecipientName())
                .recipientPhone(order.getRecipientPhone())
                .items(List.of(OrderResponse.OrderItemResponse.builder()
                        .id(detail.getId()).productName("Hoa đặt theo yêu cầu")
                        .price(quotedPrice).quantity(1).itemTotal(quotedPrice).build()))
                .createdDate(order.getCreatedDate())
                .build();
    }

    private record CustomPrice(BigDecimal amount,Integer depositPercent) {}
    private CustomPrice customPrice(CustomOrderRequest custom) {
        try {
            var design=new com.fasterxml.jackson.databind.ObjectMapper().readTree(custom.getDesiredComponents()==null?"{}":custom.getDesiredComponents());
            BigDecimal amount=design.path("quotedPrice").isNumber()?design.path("quotedPrice").decimalValue():null;
            Integer percent=design.path("depositPercent").isInt()?design.path("depositPercent").intValue():null;
            if(amount==null || amount.signum()<=0 || (percent!=null && (percent<10 || percent>100)))
                throw ApiException.badRequest("Yêu cầu chưa có báo giá hoặc mức cọc hợp lệ của shop.");
            return new CustomPrice(amount,percent);
        } catch(com.fasterxml.jackson.core.JsonProcessingException e) {
            throw ApiException.badRequest("Báo giá thiết kế không hợp lệ.");
        }
    }

    @Transactional(readOnly=true)
    public com.example.flowershop.dto.order.CustomPaymentPreview previewCustomVouchers(String userId,String requestId,com.example.flowershop.dto.order.MakeCustomOrderRequest request) {
        var custom=entityManager.find(CustomOrderRequest.class,requestId);
        if(custom==null || !custom.getCustomer().getId().equals(userId)) throw ApiException.notFound("Không tìm thấy yêu cầu hoa tùy chỉnh.");
        if(custom.getStatus()!=com.example.flowershop.entity.enums.CustomOrderStatus.ACCEPTED)
            throw ApiException.badRequest("Shop chưa chấp nhận yêu cầu này.");
        if(custom.getShop().getStatus()!=ShopStatus.ACTIVE) throw ApiException.badRequest("Cửa hàng hiện không hoạt động.");
        if(orderRepository.existsByCustomRequestIdAndStatusNot(requestId,OrderStatus.CANCELLED))
            throw ApiException.badRequest("Yêu cầu này đã có đơn đặt cọc. Hãy xem đơn hàng để tiếp tục thanh toán.");
        var price=customPrice(custom);
        var fee=quoteFee(custom.getShop(),resolveDestination(userId,request.getDeliveryAddressId()));
        var preview=vouchers.preview(new CouponService.Context(custom.getShop().getId(),price.amount(),fee,java.util.Map.of()),
                request.getCouponCode(),request.getShippingCouponCode());
        return com.example.flowershop.dto.order.CustomPaymentPreview.from(preview,price.amount(),fee,price.depositPercent());
    }

    /**
     * Tạo đơn hàng từ các cart items đã chọn.
     * Flow:
     *  1. Validate user, address, cart items
     *  2. Tính toán subTotal, totalAmount
     *  3. Tạo Order
     *  4. Tạo OrderDetail cho mỗi sản phẩm
     *  5. Giảm stock sản phẩm
     *  6. Xóa cart items ngay khi tạo đơn (cả COD và ONLINE); đơn bị hủy/hết hạn thì khôi phục lại giỏ
     *  7. COD: tạo Payment PENDING và chuyển đơn sang PROCESSING; ONLINE: giữ PENDING chờ thanh toán SePay
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
        selectedCartItems.sort(Comparator.comparing(item -> item.getProduct().getId()));
        for (CartItem cartItem : selectedCartItems) {
            Product product = productRepository.findByIdForUpdate(cartItem.getProduct().getId())
                    .orElseThrow(() -> ApiException.notFound("Sản phẩm không còn tồn tại."));
            cartItem.setProduct(product);
            if (product.getStatus() != ProductStatus.ACTIVE || product.getShop().getStatus() != ShopStatus.ACTIVE) {
                throw ApiException.badRequest("Sản phẩm hoặc cửa hàng không còn hoạt động.");
            }
            quota.requireAllowed(product);
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

        // Phí vận chuyển do GHN tính; khách trả tiền hàng + phí này
        Shop shop = selectedCartItems.get(0).getProduct().getShop();
        BigDecimal shippingFee = quoteFee(shop, deliveryAddress);
        BigDecimal totalAmount = subTotal.add(shippingFee);

        // 5. Tạo Order
        Order order = Order.builder()
                .id(UUID.randomUUID().toString())
                .customer(user)
                .shop(shop)
                .deliveryAddress(deliveryAddress)
                .recipientName(recipientName)
                .recipientPhone(recipientPhone)

                .orderType(OrderType.STANDARD)
                .subTotal(subTotal)
                .shippingFee(shippingFee)
                .totalAmount(totalAmount)
                .depositAmount(BigDecimal.ZERO)
                .status(OrderStatus.PENDING)
                .createdBy(userId)
                .lastModifyBy(userId)
                .build();

        var voucherResult=vouchers.apply(order,voucherContext(request.getShopId(),selectedCartItems,shippingFee),request.getCouponId(),request.getShippingCouponCode());
        BigDecimal discountAmount=voucherResult.discountAmount(); totalAmount=voucherResult.totalAmount();
        if(request.getExpectedTotal()!=null && request.getExpectedTotal().compareTo(totalAmount)!=0) throw ApiException.badRequest("Giá hoặc ưu đãi đã thay đổi. Vui lòng kiểm tra lại tổng thanh toán.");
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
                    .productName(product.getName())
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

        // Giỏ được xóa ngay khi tạo đơn (cả COD và online); đơn bị hủy/hết hạn thì khôi phục lại giỏ.
        cartItemRepository.deleteAll(selectedCartItems);

        // 7. Xử lý phương thức thanh toán
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

            // Đơn COD ở lại PENDING cho tới khi shop xác nhận; bộ quét hết hạn bỏ qua đơn có payment COD.
            notificationService.notifyOrderPlaced(order);
        } else {
            notificationService.notifyPaymentPending(order);
        }

        entityManager.flush(); // @CreationTimestamp chỉ được gán khi flush; thiếu dòng này response trả createdDate=null
        return OrderResponse.builder()
                .id(order.getId())
                .status(order.getStatus().name())
                .orderType(order.getOrderType().name())
                .subTotal(subTotal)
                .discountAmount(discountAmount)
                .shippingDiscountAmount(order.getShippingDiscountAmount())
                .couponCode(order.getCouponCode()).shippingCouponCode(order.getShippingCouponCode())
                .shippingFee(shippingFee)
                .totalAmount(totalAmount)
                .deliveryAddressId(deliveryAddress.getId())
                .recipientName(order.getRecipientName())
                .recipientPhone(order.getRecipientPhone())
                .items(orderItemResponses)
                .createdDate(order.getCreatedDate())
                .build();
    }

    /**
     * Khôi phục giỏ từ chi tiết đơn bị hủy, kể cả đơn cũ đã xóa giỏ lúc tạo đơn.
     * Với mỗi sản phẩm: chưa có trong giỏ thì tạo lại CartItem; đã có nhưng ít hơn thì nâng lên số lượng đã đặt.
     * Không làm gì nếu đơn không ở trạng thái CANCELLED.
     */
    @Transactional
    public void restoreCancelledOrderItems(Order order) {
        if (order == null) return;
        restoreCancelledOrderItems(order, orderDetailRepository.findByOrderId(order.getId()), List.of());
    }

    /** Như trên nhưng dùng chi tiết đơn đã đọc sẵn và bỏ qua các sản phẩm trong skipProductIds (vd: món khách đang đặt lại). */
    private void restoreCancelledOrderItems(Order order, List<OrderDetail> details, java.util.Collection<String> skipProductIds) {
        if (order == null || order.getStatus() != OrderStatus.CANCELLED || order.getCustomer() == null) return;
        String userId = order.getCustomer().getId();
        for (OrderDetail detail : details) {
            if (detail.getProduct() == null) continue;
            String productId = detail.getProduct().getId();
            if (skipProductIds.contains(productId)) continue;
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

    /**
     * Hỏi GHN phí ship từ địa chỉ lấy hàng của shop (ưu tiên địa chỉ mặc định, không thì địa chỉ đầu tiên)
     * tới địa chỉ khách. Lỗi 400 nếu shop chưa có địa chỉ với mã quận/phường GHN.
     */
    private BigDecimal quoteFee(Shop shop, Address destination) {
        List<Address> list = addressRepository.findByShopIdAndUserIsNullOrderByCreatedDateAscIdAsc(shop.getId());
        Address from = list.stream().filter(a -> Boolean.TRUE.equals(a.getIsDefault())).findFirst()
                .orElse(list.isEmpty() ? null : list.get(0));
        if (from == null || from.getGhnDistrictId() == null || from.getGhnWardCode() == null)
            throw ApiException.badRequest("Cửa hàng chưa có địa chỉ lấy hàng theo danh mục GHN nên chưa tính được phí vận chuyển.");
        return ghn.calculateFee(from.getGhnDistrictId(), from.getGhnWardCode(),
                destination.getGhnDistrictId(), destination.getGhnWardCode(), GhnService.DEFAULT_WEIGHT_GRAM);
    }

    /** Lấy địa chỉ đã lưu của khách (phải thuộc đúng user) và bắt buộc có mã quận + phường GHN để tính phí. */
    private Address resolveDestination(String userId, String addressId) {
        Address address = addressRepository.findById(addressId == null ? "" : addressId.trim())
                .filter(a -> a.getUser() != null && a.getUser().getId().equals(userId))
                .orElseThrow(() -> ApiException.badRequest("Vui lòng chọn một địa chỉ đã lưu trong tài khoản."));
        if (address.getGhnDistrictId() == null || address.getGhnWardCode() == null)
            throw ApiException.badRequest("Địa chỉ chưa có mã quận/huyện và phường/xã GHN. Hãy lưu lại địa chỉ trong hồ sơ.");
        return address;
    }

    /** Trả giá trị đầu tiên không rỗng (đã trim), hoặc null nếu cả hai đều rỗng. */
    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) return first.trim();
        if (second != null && !second.isBlank()) return second.trim();
        return null;
    }

    /**
     * Tự động hủy đơn hàng quá hạn chưa thanh toán (PENDING 10 phút, AWAITING_DEPOSIT 24 giờ).
     * Hoàn lại tồn kho cho từng sản phẩm, hoàn lại coupon (nếu có), và hủy các payment pending.
     */
    @Transactional
    public void cancelExpiredOrder(String orderId, String reason) {
        var current=orderRepository.findById(orderId).orElse(null);
        if(current!=null && current.getPaymentWindowStart()!=null) {
            var deadline=current.getStatus()==OrderStatus.AWAITING_DEPOSIT
                    ? current.getPaymentWindowStart().plusMinutes(SePayGatewayService.DEPOSIT_DEADLINE_MINUTES)
                    : current.getPaymentWindowStart().plusMinutes(SePayGatewayService.PAYMENT_WINDOW_MINUTES);
            if(deadline.isAfter(LocalDateTime.now())) return;
        }
        cancelPendingOrder(orderId, reason, "SYSTEM_AUTO_CANCEL");
    }

    /**
     * Hủy đơn chờ thanh toán do khách/hệ thống yêu cầu (có khôi phục giỏ).
     * @return true nếu đơn thực sự bị hủy (false nếu đơn đã được thanh toán/xử lý trước đó).
     */
    @Transactional
    public boolean cancelPendingOrder(String orderId, String reason, String actor) {
        boolean cancelled = cancelPending(orderId, reason, actor, List.of());
        // Không báo ở cancelPending: nó cũng được gọi khi khách đặt lại món (không phải thanh toán thất bại).
        if (cancelled) orderRepository.findById(orderId).ifPresent(o -> notificationService.notifyPaymentFailed(o, reason));
        return cancelled;
    }

    /**
     * Lõi hủy đơn PENDING/AWAITING_DEPOSIT: khóa đơn và đọc lại từ DB (tránh race với lần đối soát SePay ghi nhận tiền),
     * đặt CANCELLED, hoàn kho, hoàn lượt coupon, đánh fail các Payment PENDING, và khôi phục giỏ (trừ các sản phẩm trong skipRestoreProductIds).
     * @return false nếu không có đơn hoặc đơn đã sang trạng thái khác (đã thanh toán...)
     */
    private boolean cancelPending(String orderId, String reason, String actor, java.util.Collection<String> skipRestoreProductIds) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) return false;
        // Khóa và đọc lại từ DB: entity trong persistence context có thể đã cũ so với lần đối soát vừa commit.
        entityManager.flush();
        entityManager.refresh(order, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.AWAITING_DEPOSIT) {
            return false;
        }

        log.info("Hủy đơn hàng chờ thanh toán: #{} (Lý do: {})", order.getId(), reason);

        // 1. Cập nhật trạng thái Order sang CANCELLED
        order.setStatus(OrderStatus.CANCELLED);
        order.setLastModifyBy(actor);
        orderRepository.save(order);

        List<OrderDetail> details = releaseStockAndCoupon(order, actor);

        // 4. Hủy các payment PENDING của đơn hàng
        paymentRepository.failPendingByOrderId(order.getId(), actor);
        restoreCancelledOrderItems(order, details, skipRestoreProductIds);
        return true;
    }

    /** Đơn chưa giao cho GHN thì khách còn hủy được. */
    private static final java.util.Set<OrderStatus> CUSTOMER_CANCELLABLE = java.util.EnumSet.of(
            OrderStatus.PENDING, OrderStatus.AWAITING_DEPOSIT, OrderStatus.DEPOSIT_PAID, OrderStatus.PROCESSING,OrderStatus.AWAITING_QUOTE,OrderStatus.QUOTED);

    /**
     * Khách tự hủy đơn trước khi giao cho GHN.
     *  1. Khóa đơn (FOR UPDATE) để không chạy đua với đối soát thanh toán / shop gửi GHN
     *  2. Chỉ chủ đơn được hủy (đơn người khác → 404), chỉ hủy khi chưa giao
     *  3. Trả kho + lượt coupon, hủy các payment còn PENDING
     *  4. Đã trả tiền (cọc hoặc đủ) → tạo Refund REQUESTED và báo staff hoàn tiền
     */
    @Transactional
    public void cancelByCustomer(String userId, String orderId, String reason) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .filter(o -> o.getCustomer().getId().equals(userId))
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy đơn hàng."));
        if (!CUSTOMER_CANCELLABLE.contains(order.getStatus())) {
            throw ApiException.badRequest("Đơn đã giao cho đơn vị vận chuyển hoặc đã kết thúc, không thể hủy.");
        }
        String why = "Khách hủy đơn" + (reason == null || reason.isBlank() ? "." : ": " + reason.trim());
        BigDecimal paid = paymentRepository.findByOrderId(orderId).stream()
                .filter(p -> p.getStatus() == PaymentStatus.SUCCESS)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelReason(why);
        order.setLastModifyBy(userId);
        orderRepository.save(order);
        releaseStockAndCoupon(order, userId);
        paymentRepository.failPendingByOrderId(orderId, userId);

        if (paid.signum() > 0) {
            Refund refund = Refund.builder().id(UUID.randomUUID().toString())
                    .order(order).amount(paid).reason(why).createdBy(userId).lastModifyBy(userId).build();
            refundRepository.save(refund);
            notificationService.notifyRefundRequested(refund);
        }
    }

    /** Trả lại kho cho các món có sẵn trong đơn và trả lại 1 lượt dùng coupon. Trả về chi tiết đơn đã đọc để nơi gọi dùng lại. */
    private List<OrderDetail> releaseStockAndCoupon(Order order, String actor) {
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
        vouchers.release(order);
        return details;
    }
    private CouponService.Context voucherContext(String shopId, List<CartItem> items, BigDecimal shippingFee) {
        var totals = new java.util.HashMap<String, BigDecimal>();
        for (var item : items) totals.merge(item.getProduct().getId(), item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())), BigDecimal::add);
        return new CouponService.Context(shopId, totals.values().stream().reduce(BigDecimal.ZERO,BigDecimal::add),shippingFee,totals);
    }
    public CouponService.Preview previewVouchers(String userId, MakeOrderRequest request) {
        return previewVouchers(userId,request,false);
    }
    @Transactional(readOnly=true)
    public CouponService.Preview previewVouchers(String userId, MakeOrderRequest request, boolean merchandiseOnly) {
        if(merchandiseOnly && request.getShippingCouponCode()!=null && !request.getShippingCouponCode().isBlank())
            throw ApiException.badRequest("Cần tính phí giao hàng trước khi áp mã phí ship.");
        if (new HashSet<>(request.getCartItemIds()).size()!=request.getCartItemIds().size()) throw ApiException.badRequest("Danh sách sản phẩm bị trùng.");
        var items = request.getCartItemIds().stream().map(id -> cartItemRepository.findById(id)
                .filter(c -> c.getUser().getId().equals(userId) && c.getProduct().getShop().getId().equals(request.getShopId()))
                .filter(c -> c.getProduct().getStatus()==ProductStatus.ACTIVE && c.getProduct().getShop().getStatus()==ShopStatus.ACTIVE)
                .orElseThrow(() -> ApiException.badRequest("Sản phẩm không thuộc giỏ hàng/shop đang chọn hoặc đã ngừng bán."))).toList();
        if(items.isEmpty()) throw ApiException.badRequest("Chọn sản phẩm để áp voucher.");
        items.forEach(item->quota.requireAllowed(item.getProduct()));
        var shippingFee=merchandiseOnly ? BigDecimal.ZERO : quoteFee(items.get(0).getProduct().getShop(),resolveDestination(userId,request.getDeliveryAddressId()));
        return vouchers.preview(voucherContext(request.getShopId(),items,shippingFee), request.getCouponId(),request.getShippingCouponCode());
    }
    @Transactional(readOnly=true)
    public BigDecimal quoteShippingFee(String userId, ShippingFeeRequest request) {
        Shop shop = shopRepository.findById(request.getShopId())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy cửa hàng."));
        return quoteFee(shop, resolveDestination(userId, request.getDeliveryAddressId()));
    }
    @Transactional(readOnly=true)
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
                            .productName(java.util.Objects.requireNonNullElse(d.snapshotName(), "Sản phẩm custom"))
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
                    .shippingDiscountAmount(order.getShippingDiscountAmount())
                    .couponCode(order.getCouponCode()).shippingCouponCode(order.getShippingCouponCode())
                    .totalAmount(order.getTotalAmount())
                    .deliveryAddressId(order.getDeliveryAddress()==null ? null : order.getDeliveryAddress().getId())
                    .recipientName(order.getRecipientName())
                    .recipientPhone(order.getRecipientPhone())
                    .items(items)
                    .createdDate(order.getCreatedDate())
                    .build();
        }).collect(Collectors.toList());
    }
}

