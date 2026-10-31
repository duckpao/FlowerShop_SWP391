package com.example.flowershop.controller;

import com.example.flowershop.dto.auth.CurrentUser;
import com.example.flowershop.service.CustomOrderWorkflowService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class CustomOrderWorkflowController {
    private final CustomOrderWorkflowService workflow;
    public CustomOrderWorkflowController(CustomOrderWorkflowService workflow) { this.workflow=workflow; }

    @PostMapping("/api/customer/orders/custom")
    @PreAuthorize("hasRole('CUSTOMER')")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomOrderWorkflowService.Receipt create(@AuthenticationPrincipal CurrentUser user,
            @Valid @RequestBody CustomOrderWorkflowService.CreateInput body) { return workflow.create(user.id(), body); }

    @PostMapping("/api/shop/mine/{shopId}/orders/{id}/quote")
    @PreAuthorize("hasRole('SHOP')")
    public void quote(@PathVariable("shopId") String shopId, @PathVariable("id") String id,
            @AuthenticationPrincipal CurrentUser user, @Valid @RequestBody CustomOrderWorkflowService.Quote body) {
        workflow.quote(shopId, user.id(), id, body);
    }

    @PostMapping("/api/customer/orders/{id}/accept-quote")
    @PreAuthorize("hasRole('CUSTOMER')")
    public CustomOrderWorkflowService.Receipt accept(@PathVariable("id") String id,
            @AuthenticationPrincipal CurrentUser user, @Valid @RequestBody CustomOrderWorkflowService.Accept body) {
        return workflow.accept(user.id(), id, body);
    }
    @PutMapping("/api/customer/orders/{id}/custom-address")
    @PreAuthorize("hasRole('CUSTOMER')")
    public void address(@PathVariable("id") String id, @AuthenticationPrincipal CurrentUser user,
            @Valid @RequestBody CustomOrderWorkflowService.DeliveryInput body) { workflow.deliveryAddress(user.id(), id, body); }
}
