package com.example.flowershop.controller;

import com.example.flowershop.service.AdminOrderService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Admin xem đơn hàng của mọi shop (chỉ đọc). */
@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
public class AdminOrderController {
    private final AdminOrderService service;

    public AdminOrderController(AdminOrderService service) {
        this.service = service;
    }

    @GetMapping
    public AdminOrderService.OrderPage list(@RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        return service.list(status, q, from, to, page, size);
    }

    @GetMapping("/{id}")
    public AdminOrderService.AdminOrderDetail detail(@PathVariable(name = "id") String id) {
        return service.detail(id);
    }
}
