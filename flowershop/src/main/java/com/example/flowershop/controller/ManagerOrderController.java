package com.example.flowershop.controller;

import com.example.flowershop.dto.auth.CurrentUser;
import com.example.flowershop.service.ManagerOrderService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shop/mine/{shopId}/orders")
@PreAuthorize("hasRole('SHOP')")
@SecurityRequirement(name = "bearerAuth")
public class ManagerOrderController {
    private final ManagerOrderService service;
    public ManagerOrderController(ManagerOrderService service) { this.service = service; }

    @GetMapping
    public ManagerOrderService.Results list(@PathVariable String shopId, @AuthenticationPrincipal CurrentUser u,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(required = false) String status) {
        return service.list(shopId, u.id(), page, status);
    }

    @GetMapping("/{id}")
    public ManagerOrderService.OrderDetailResponse detail(@PathVariable String shopId, @PathVariable String id,
                                                          @AuthenticationPrincipal CurrentUser u) {
        return service.detail(shopId, u.id(), id);
    }

    @PostMapping("/{id}/ship")
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
    public ManagerOrderService.ShipResult ship(@PathVariable String shopId, @PathVariable String id,
                                               @AuthenticationPrincipal CurrentUser u) {
        return service.ship(shopId, u.id(), id);
    }

    @PostMapping("/{id}/refresh-status")
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
    public String refreshStatus(@PathVariable String shopId, @PathVariable String id,
                                 @AuthenticationPrincipal CurrentUser u) {
        return service.refreshStatus(shopId, u.id(), id).name();
    }

    @GetMapping("/queue")
    public ManagerOrderService.Results queue(@PathVariable String shopId, @AuthenticationPrincipal CurrentUser u,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(required = false) String status) {
        return service.queue(shopId, u.id(), page, status);
    }

    @GetMapping("/queue/count")
    public ManagerOrderService.QueueCount queueCount(@PathVariable String shopId, @AuthenticationPrincipal CurrentUser u) {
        return service.queueCount(shopId, u.id());
    }

    @PostMapping("/{id}/cancel")
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
    public void cancel(@PathVariable String shopId, @PathVariable String id,
                       @RequestBody java.util.Map<String, String> body,
                       @AuthenticationPrincipal CurrentUser u) {
        service.cancel(shopId, u.id(), id, body.get("reason"));
    }
}