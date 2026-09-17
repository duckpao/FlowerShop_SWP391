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
import com.example.flowershop.repository.CartItemRepository;
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
    private final UserRepository userRepository;

    /**
     * Thêm sản phẩm vào giỏ hàng.
     * Nếu sản phẩm đã tồn tại trong giỏ → cộng dồn số lượng.
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

        // 3. Kiểm tra stock
        if (product.getStock() < request.getQuantity()) {
            throw ApiException.badRequest(
                    "Số lượng yêu cầu vượt quá tồn kho. Tồn kho hiện tại: " + product.getStock()
            );
        }

        // 4. Nếu sản phẩm đã có trong giỏ → cộng dồn số lượng
        Optional<CartItem> existingItem = cartItemRepository.findByUserIdAndProductId(userId, request.getProductId());

        CartItem cartItem;
        if (existingItem.isPresent()) {
            cartItem = existingItem.get();
            int newQuantity = cartItem.getQuantity() + request.getQuantity();
            // Kiểm tra lại stock sau khi cộng dồn
            if (product.getStock() < newQuantity) {
                throw ApiException.badRequest(
                        "Tổng số lượng trong giỏ vượt quá tồn kho. Tồn kho: " + product.getStock()
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
     * Lấy toàn bộ giỏ hàng của user.
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
     * Cập nhật số lượng của 1 cart item.
     */
    @Transactional
    public CartItemResponse updateCartItem(String userId, String cartItemId, UpdateCartItemRequest request) {
        CartItem cartItem = getCartItemOrThrow(cartItemId, userId);

        // Kiểm tra stock
        Product product = cartItem.getProduct();
        if (product.getStock() < request.getQuantity()) {
            throw ApiException.badRequest(
                    "Số lượng yêu cầu vượt quá tồn kho. Tồn kho hiện tại: " + product.getStock()
            );
        }

        cartItem.setQuantity(request.getQuantity());
        cartItem.setLastModifyBy(userId);
        cartItem = cartItemRepository.save(cartItem);
        return toCartItemResponse(cartItem);
    }

    /**
     * Xóa 1 sản phẩm khỏi giỏ hàng.
     */
    @Transactional
    public void removeCartItem(String userId, String cartItemId) {
        CartItem cartItem = getCartItemOrThrow(cartItemId, userId);
        cartItemRepository.delete(cartItem);
    }

    /**
     * Xóa nhiều cart items theo danh sách ID (dùng sau khi đặt hàng thành công).
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

    private CartItem getCartItemOrThrow(String cartItemId, String userId) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy cart item với id: " + cartItemId));

        // Kiểm tra cart item có thuộc về user này không
        if (!cartItem.getUser().getId().equals(userId)) {
            throw ApiException.forbidden("Bạn không có quyền truy cập cart item này.");
        }
        return cartItem;
    }

    CartItemResponse toCartItemResponse(CartItem cartItem) {
        Product product = cartItem.getProduct();
        BigDecimal itemTotal = product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));

        CartItemResponse.ProductSummary productSummary = CartItemResponse.ProductSummary.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .images(product.getImages())
                .stock(product.getStock())
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

