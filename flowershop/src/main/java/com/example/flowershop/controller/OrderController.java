package com.example.flowershop.controller;

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

    /** Chặn truy cập chéo: userId trên URL phải trùng user đang đăng nhập (JWT), nếu không trả 403. */
    private static void requireOwner(String userId, CurrentUser currentUser) {
        if (currentUser == null || !userId.equals(currentUser.id())) throw ApiException.forbidden("Không có quyền truy cập đơn hàng này.");
    }

    /**
     * Đặt nhiều đơn một lúc (mỗi shop một đơn, kiểu giỏ hàng nhiều shop) — đây là API checkout đang dùng.
     * Cùng một transaction: lỗi ở shop nào thì cả nhóm rollback.
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
     * Ước tính phí giao hàng GHN cho checkout (bản GET, trả {"shippingFee": số tiền}).
     * GET /api/orders/{userId}/shipping-fee?shopId=...&addressId=...
     */
    @GetMapping("/{userId}/shipping-fee")
    public ResponseEntity<java.util.Map<String, java.math.BigDecimal>> estimateShippingFee(
            @PathVariable String userId, @RequestParam String shopId, @RequestParam String addressId,
            @AuthenticationPrincipal CurrentUser currentUser) {
        requireOwner(userId, currentUser);
        return ResponseEntity.ok(java.util.Map.of("shippingFee", orderService.estimateShippingFee(userId, shopId, addressId)));
    }

    /**
     * Lịch sử đơn hàng của user, mới nhất trước.
     * GET /api/orders/{userId}
     */
    @GetMapping("/{userId}")
    public ResponseEntity<List<OrderResponse>> getOrderHistory(@PathVariable String userId, @AuthenticationPrincipal CurrentUser currentUser) {
        requireOwner(userId, currentUser);
        return ResponseEntity.ok(orderService.getOrderHistory(userId));
    }
}

