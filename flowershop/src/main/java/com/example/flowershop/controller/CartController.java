package com.example.flowershop.controller;

import com.example.flowershop.dto.cart.AddToCartRequest;
import com.example.flowershop.dto.cart.CartItemResponse;
import com.example.flowershop.dto.cart.CartResponse;
import com.example.flowershop.dto.cart.UpdateCartItemRequest;
import com.example.flowershop.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    /**
     * Thêm sản phẩm vào giỏ hàng.
     * POST /api/cart/{userId}/add
     */
    @PostMapping("/{userId}/add")
    public ResponseEntity<CartItemResponse> addToCart(
            @PathVariable String userId,
            @Valid @RequestBody AddToCartRequest request
    ) {
        CartItemResponse response = cartService.addToCart(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Xem toàn bộ giỏ hàng của user.
     * GET /api/cart/{userId}
     */
    @GetMapping("/{userId}")
    public ResponseEntity<CartResponse> getCart(@PathVariable String userId) {
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    /**
     * Cập nhật số lượng 1 cart item.
     * PUT /api/cart/{userId}/items/{itemId}
     */
    @PutMapping("/{userId}/items/{itemId}")
    public ResponseEntity<CartItemResponse> updateCartItem(
            @PathVariable String userId,
            @PathVariable String itemId,
            @Valid @RequestBody UpdateCartItemRequest request
    ) {
        return ResponseEntity.ok(cartService.updateCartItem(userId, itemId, request));
    }

    /**
     * Xóa 1 sản phẩm khỏi giỏ hàng.
     * DELETE /api/cart/{userId}/items/{itemId}
     */
    @DeleteMapping("/{userId}/items/{itemId}")
    public ResponseEntity<Void> removeCartItem(
            @PathVariable String userId,
            @PathVariable String itemId
    ) {
        cartService.removeCartItem(userId, itemId);
        return ResponseEntity.noContent().build();
    }
}

