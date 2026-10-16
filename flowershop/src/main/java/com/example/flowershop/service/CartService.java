package com.example.flowershop.service;

import com.example.flowershop.dto.cart.AddToCartRequest;
import com.example.flowershop.dto.cart.CartItemResponse;
import com.example.flowershop.dto.cart.CartResponse;
import com.example.flowershop.dto.cart.UpdateCartItemRequest;
import com.example.flowershop.entity.CartItem;
import com.example.flowershop.entity.Product;
import com.example.flowershop.entity.User;
import com.example.flowershop.exception.ApiException;
import com.example.flowershop.entity.enums.ProductStatus;
import com.example.flowershop.entity.enums.ProductType;
import com.example.flowershop.entity.ProductImage;
import com.example.flowershop.repository.CartItemRepository;
import com.example.flowershop.repository.OrderDetailRepository;
import com.example.flowershop.repository.ProductImageRepository;
import com.example.flowershop.repository.ProductRepository;
import com.example.flowershop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final UserRepository userRepository;
    private final OrderDetailRepository orderDetailRepository;

    /** Kho khách này có thể đặt: kho còn lại cộng phần chính họ đang giữ trong đơn PENDING chưa thanh toán. */
    private int availableStock(String userId, Product product) {
        return product.getStock() + orderDetailRepository.sumPendingQuantity(userId, product.getId());
    }

    /**
     * Thêm sản phẩm vào giỏ hàng.
     * Các bước: kiểm tra user -> kiểm tra sản phẩm tồn tại và không INACTIVE -> kiểm tra tồn kho
     * (bỏ qua với hàng CUSTOM) -> nếu đã có trong giỏ thì cộng dồn (kiểm tra lại tồn kho với tổng mới),
     * nếu chưa thì tạo CartItem mới.
     * @throws ApiException 404 nếu không có user/sản phẩm; 400 nếu hết hàng hoặc vượt tồn kho
     */
    @Transactional
    public CartItemResponse addToCart(String userId, AddToCartRequest request) {
        // 1. Kiểm tra user tồn tại
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy user với id: " + userId));

        // 2. Kiểm tra sản phẩm tồn tại và hợp lệ
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy sản phẩm với id: " + request.getProductId()));

        if (product.getStatus() == ProductStatus.INACTIVE) {
            throw ApiException.badRequest("Sản phẩm này hiện không còn bán.");
        }

        // 3. Kiểm tra stock (chỉ áp dụng với hoa có sẵn)
        int available = availableStock(userId, product);
        if (product.getType() != ProductType.CUSTOM && available < request.getQuantity()) {
            throw ApiException.badRequest(
                    "Số lượng yêu cầu vượt quá tồn kho. Tồn kho hiện tại: " + available
            );
        }

        // 4. Nếu sản phẩm đã có trong giỏ → cộng dồn số lượng
        Optional<CartItem> existingItem = cartItemRepository.findByUserIdAndProductId(userId, request.getProductId());

        CartItem cartItem;
        if (existingItem.isPresent()) {
            cartItem = existingItem.get();
            int newQuantity = cartItem.getQuantity() + request.getQuantity();
            // Kiểm tra lại stock sau khi cộng dồn (chỉ áp dụng với hoa có sẵn)
            if (product.getType() != ProductType.CUSTOM && available < newQuantity) {
                throw ApiException.badRequest(
                        "Tổng số lượng trong giỏ vượt quá tồn kho. Tồn kho: " + available
                        + ", Hiện trong giỏ: " + cartItem.getQuantity()
                );
            }
            cartItem.setQuantity(newQuantity);
            cartItem.setLastModifyBy(userId);
        } else {
            // 5. Tạo mới cart item
            cartItem = CartItem.builder()
                    .id(UUID.randomUUID().toString())
                    .user(user)
                    .product(product)
                    .quantity(request.getQuantity())
                    .createdBy(userId)
                    .lastModifyBy(userId)
                    .build();
        }

        cartItem = cartItemRepository.save(cartItem);
        return toCartItemResponse(cartItem);
    }

    /**
     * Lấy toàn bộ giỏ hàng của user: map từng CartItem sang response (kèm ảnh, tồn kho, thành tiền)
     * và tính totalItems (số dòng), totalQuantity (tổng số lượng), grandTotal (tổng tiền theo giá hiện tại).
     */
    @Transactional(readOnly = true)
    public CartResponse getCart(String userId) {
        // Kiểm tra user tồn tại
        if (!userRepository.existsById(userId)) {
            throw ApiException.notFound("Không tìm thấy user với id: " + userId);
        }

        List<CartItem> items = cartItemRepository.findByUserId(userId);

        List<CartItemResponse> itemResponses = items.stream()
                .map(this::toCartItemResponse)
                .collect(Collectors.toList());

        int totalQuantity = items.stream().mapToInt(CartItem::getQuantity).sum();

        BigDecimal grandTotal = itemResponses.stream()
                .map(CartItemResponse::getItemTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .items(itemResponses)
                .totalItems(items.size())
                .totalQuantity(totalQuantity)
                .grandTotal(grandTotal)
                .build();
    }

    /**
     * Đặt lại số lượng của 1 cart item. Kiểm tra item thuộc về user, rồi kiểm tra số lượng mới
     * không vượt tồn kho khả dụng (bỏ qua với hàng CUSTOM).
     */
    @Transactional
    public CartItemResponse updateCartItem(String userId, String cartItemId, UpdateCartItemRequest request) {
        CartItem cartItem = getCartItemOrThrow(cartItemId, userId);

        // Kiểm tra stock (chỉ áp dụng với hoa có sẵn)
        Product product = cartItem.getProduct();
        int available = availableStock(userId, product);
        if (product.getType() != ProductType.CUSTOM && available < request.getQuantity()) {
            throw ApiException.badRequest(
                    "Số lượng yêu cầu vượt quá tồn kho. Tồn kho hiện tại: " + available
            );
        }

        cartItem.setQuantity(request.getQuantity());
        cartItem.setLastModifyBy(userId);
        cartItem = cartItemRepository.save(cartItem);
        return toCartItemResponse(cartItem);
    }

    /** Xóa 1 cart item khỏi giỏ (sau khi xác nhận item thuộc về user). */
    @Transactional
    public void removeCartItem(String userId, String cartItemId) {
        CartItem cartItem = getCartItemOrThrow(cartItemId, userId);
        cartItemRepository.delete(cartItem);
    }

    /**
     * Xóa nhiều cart items theo danh sách ID (dùng sau khi đặt hàng thành công).
     * Chỉ xóa item thuộc về user, id lạ hoặc của người khác bị bỏ qua âm thầm.
     */
    @Transactional
    public void clearCartItems(String userId, List<String> cartItemIds) {
        for (String id : cartItemIds) {
            if (cartItemRepository.existsByIdAndUserId(id, userId)) {
                cartItemRepository.deleteById(id);
            }
        }
    }

    // ===================== HELPER METHODS =====================

    /** Lấy CartItem theo id; 404 nếu không có, 403 nếu item thuộc user khác. */
    private CartItem getCartItemOrThrow(String cartItemId, String userId) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy cart item với id: " + cartItemId));

        // Kiểm tra cart item có thuộc về user này không
        if (!cartItem.getUser().getId().equals(userId)) {
            throw ApiException.forbidden("Bạn không có quyền truy cập cart item này.");
        }
        return cartItem;
    }

    /**
     * Chuyển CartItem sang DTO trả về frontend: tính thành tiền = giá hiện tại x số lượng,
     * kèm danh sách ảnh (theo displayOrder), tồn kho khả dụng và thông tin shop để FE gom đơn theo shop.
     */
    CartItemResponse toCartItemResponse(CartItem cartItem) {
        Product product = cartItem.getProduct();
        BigDecimal itemTotal = product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));

        List<String> imageUrls = productImageRepository.findByProductIdOrderByDisplayOrderAsc(product.getId())
                .stream()
                .map(ProductImage::getImageUrl)
                .toList();

        CartItemResponse.ProductSummary productSummary = CartItemResponse.ProductSummary.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .images(imageUrls)
                .image(imageUrls.isEmpty() ? null : imageUrls.get(0))
                .stock(availableStock(cartItem.getUser().getId(), product))
                .shopId(product.getShop().getId())
                .shopName(product.getShop().getName())
                .build();

        return CartItemResponse.builder()
                .id(cartItem.getId())
                .product(productSummary)
                .quantity(cartItem.getQuantity())
                .itemTotal(itemTotal)
                .build();
    }
}

