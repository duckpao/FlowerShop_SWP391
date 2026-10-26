package com.example.flowershop.controller;

import com.example.flowershop.service.CustomFlowerOrderService;
import com.example.flowershop.entity.enums.CustomOrderStatus;
import com.example.flowershop.dto.auth.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;

@RestController
public class CustomFlowerOrderController {
    private final CustomFlowerOrderService service;
    public CustomFlowerOrderController(CustomFlowerOrderService service){this.service=service;}
    @GetMapping("/api/public/custom-orders/suggestions")
    public CustomFlowerOrderService.Suggestions suggestions(@RequestParam BigDecimal budget,
            @RequestParam(defaultValue="1") int quantity,@RequestParam(required=false) String shopId,
            @RequestParam(defaultValue="") String q,@RequestParam(defaultValue="0") int page){return service.suggestions(budget,quantity,shopId,q,page);}
    @PostMapping("/api/customer/custom-orders") @PreAuthorize("hasRole('CUSTOMER')") @ResponseStatus(HttpStatus.CREATED)
    public CustomFlowerOrderService.Result create(@AuthenticationPrincipal CurrentUser user,@Valid @RequestBody CustomFlowerOrderService.RequestInput input){return service.create(user.id(),input);}
    @GetMapping("/api/customer/custom-orders") @PreAuthorize("hasRole('CUSTOMER')")
    public CustomFlowerOrderService.Results mine(@AuthenticationPrincipal CurrentUser user,@RequestParam(defaultValue="0") int page){return service.mine(user.id(),page);}
    @GetMapping("/api/customer/custom-orders/{id}") @PreAuthorize("hasRole('CUSTOMER')")
    public CustomFlowerOrderService.Result detail(@AuthenticationPrincipal CurrentUser user,@PathVariable String id){return service.detail(user.id(),id);}
    @GetMapping("/api/shop/mine/{shopId}/custom-orders") @PreAuthorize("hasRole('SHOP')")
    public CustomFlowerOrderService.Results list(@PathVariable String shopId,@AuthenticationPrincipal CurrentUser user,@RequestParam(required=false) CustomOrderStatus status,@RequestParam(defaultValue="0") int page){return service.shopRequests(shopId,user.id(),status,page);}
    @GetMapping("/api/shop/mine/{shopId}/custom-orders/{id}") @PreAuthorize("hasRole('SHOP')")
    public CustomFlowerOrderService.Result shopDetail(@PathVariable String shopId,@PathVariable String id,@AuthenticationPrincipal CurrentUser user){return service.shopRequest(shopId,user.id(),id);}
    @PutMapping("/api/shop/mine/{shopId}/custom-orders/{id}") @PreAuthorize("hasRole('SHOP')")
    public CustomFlowerOrderService.Result decide(@PathVariable String shopId,@PathVariable String id,@AuthenticationPrincipal CurrentUser user,@Valid @RequestBody CustomFlowerOrderService.Decision input){return service.decide(shopId,user.id(),id,input);}
}
