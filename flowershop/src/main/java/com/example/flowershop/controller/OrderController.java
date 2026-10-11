package com.example.flowershop.controller;

import com.example.flowershop.dto.order.MakeOrderRequest;
import com.example.flowershop.dto.order.OrderResponse;
import com.example.flowershop.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.example.flowershop.dto.auth.CurrentUser;
import com.example.flowershop.exception.ApiException;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    private static void requireOwner(String userId, CurrentUser currentUser) {
        if (currentUser == null || !userId.equals(currentUser.id())) throw ApiException.forbidden("Không có quyền truy cập đơn hàng này.");
    }

    /**
     * Tạo đơn hàng từ các sản phẩm đã chọn trong giỏ.
     * POST /api/orders/{userId}
     */
    @PostMapping("/{userId}")
    public ResponseEntity<OrderResponse> makeOrder(
            @PathVariable String userId,
            @Valid @RequestBody MakeOrderRequest request, @AuthenticationPrincipal CurrentUser currentUser
    ) {
        requireOwner(userId, currentUser);
        OrderResponse response = orderService.makeOrder(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Đặt nhiều đơn một lúc (mỗi shop một đơn, kiểu giỏ hàng nhiều shop).
     * POST /api/orders/{userId}/batch
     */
    @PostMapping("/{userId}/batch")
    public ResponseEntity<List<OrderResponse>> makeOrders(
            @PathVariable String userId,
            @Valid @RequestBody com.example.flowershop.dto.order.MakeOrdersRequest request,
            @AuthenticationPrincipal CurrentUser currentUser) {
        requireOwner(userId, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.makeOrders(userId, request));
    }

    /**
     * Ước tính phí giao hàng GHN cho checkout.
     * GET /api/orders/{userId}/shipping-fee?shopId=...&addressId=...
     */
    @GetMapping("/{userId}/shipping-fee")
    public ResponseEntity<java.util.Map<String, java.math.BigDecimal>> shippingFee(
            @PathVariable String userId, @RequestParam String shopId, @RequestParam String addressId,
            @AuthenticationPrincipal CurrentUser currentUser) {
        requireOwner(userId, currentUser);
        return ResponseEntity.ok(java.util.Map.of("shippingFee", orderService.estimateShippingFee(userId, shopId, addressId)));
    }

    /**
     * Lịch sử đơn hàng của user.
     * GET /api/orders/{userId}
     */
    @GetMapping("/{userId}")
    public ResponseEntity<List<OrderResponse>> getOrderHistory(@PathVariable String userId, @AuthenticationPrincipal CurrentUser currentUser) {
        requireOwner(userId, currentUser);
        return ResponseEntity.ok(orderService.getOrderHistory(userId));
    }
}

