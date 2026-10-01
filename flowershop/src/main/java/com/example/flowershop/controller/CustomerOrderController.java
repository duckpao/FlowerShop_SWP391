package com.example.flowershop.controller;

import com.example.flowershop.dto.auth.CurrentUser;
import com.example.flowershop.service.CustomerOrderService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer/orders")
@PreAuthorize("hasRole('CUSTOMER')")
@SecurityRequirement(name = "bearerAuth")
public class CustomerOrderController {
    private final CustomerOrderService service;
    public CustomerOrderController(CustomerOrderService service) { this.service = service; }

    @PostMapping("/fee")
    public java.util.Map<String, Object> fee(@AuthenticationPrincipal CurrentUser user,
                                             @RequestBody CustomerOrderService.CheckoutRequest req) {
        return java.util.Map.of("shippingFee", service.calculateFee(user.id(), req));
    }

    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
    public CustomerOrderService.CheckoutResponse checkout(@AuthenticationPrincipal CurrentUser user,
                                                          @Valid @RequestBody CustomerOrderService.CheckoutRequest req) {
        return service.checkout(user.id(), req);
    }

    @GetMapping
    public CustomerOrderService.Results list(@AuthenticationPrincipal CurrentUser user,
                                             @RequestParam(defaultValue = "0") int page) {
        return service.list(user.id(), page);
    }

    @GetMapping("/{id}")
    public CustomerOrderService.OrderDetailResponse detail(@AuthenticationPrincipal CurrentUser user,
                                                           @PathVariable String id) {
        return service.detail(user.id(), id);
    }
}