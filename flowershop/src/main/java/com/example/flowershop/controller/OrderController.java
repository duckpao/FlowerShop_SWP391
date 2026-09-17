package com.example.flowershop.controller;

import com.example.flowershop.dto.order.MakeOrderRequest;
import com.example.flowershop.dto.order.OrderResponse;
import com.example.flowershop.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * Tạo đơn hàng từ các sản phẩm đã chọn trong giỏ.
     * POST /api/orders/{userId}
     */
    @PostMapping("/{userId}")
    public ResponseEntity<OrderResponse> makeOrder(
            @PathVariable String userId,
            @Valid @RequestBody MakeOrderRequest request
    ) {
        OrderResponse response = orderService.makeOrder(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Lịch sử đơn hàng của user.
     * GET /api/orders/{userId}
     */
    @GetMapping("/{userId}")
    public ResponseEntity<List<OrderResponse>> getOrderHistory(@PathVariable String userId) {
        return ResponseEntity.ok(orderService.getOrderHistory(userId));
    }
}

