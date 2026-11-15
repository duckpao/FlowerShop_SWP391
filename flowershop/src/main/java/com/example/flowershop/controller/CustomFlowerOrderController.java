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
    public CustomFlowerOrderService.Suggestions suggestions(@RequestParam("budget") BigDecimal budget,
            @RequestParam(name="quantity", defaultValue="1") int quantity,@RequestParam(name="shopId", required=false) String shopId,
            @RequestParam(name="q", defaultValue="") String q,@RequestParam(name="page", defaultValue="0") int page){return service.suggestions(budget,quantity,shopId,q,page);}
    @PostMapping("/api/customer/custom-orders") @PreAuthorize("hasRole('CUSTOMER')") @ResponseStatus(HttpStatus.CREATED)
    public CustomFlowerOrderService.Result create(@AuthenticationPrincipal CurrentUser user,@Valid @RequestBody CustomFlowerOrderService.RequestInput input){return service.create(user.id(),input);}
    @GetMapping("/api/customer/custom-orders") @PreAuthorize("hasRole('CUSTOMER')")
    public CustomFlowerOrderService.Results mine(@AuthenticationPrincipal CurrentUser user,@RequestParam(name="page", defaultValue="0") int page){return service.mine(user.id(),page);}
    @GetMapping("/api/customer/custom-orders/{id}") @PreAuthorize("hasRole('CUSTOMER')")
    public CustomFlowerOrderService.Result detail(@AuthenticationPrincipal CurrentUser user,@PathVariable("id") String id){return service.detail(user.id(),id);}
    @PostMapping("/api/customer/custom-orders/{id}/reject") @PreAuthorize("hasRole('CUSTOMER')")
    public CustomFlowerOrderService.Result customerReject(@AuthenticationPrincipal CurrentUser user,@PathVariable("id") String id,@Valid @RequestBody(required=false) CustomFlowerOrderService.Reason input){return service.customerReject(user.id(),id,input);}
    // SecurityConfig tách role theo đường dẫn (/api/staff cho staff, /api/shop cho chủ shop); guard kiểm tra đúng shop
    @GetMapping({"/api/shop/mine/{shopId}/custom-orders","/api/staff/shops/{shopId}/custom-orders"}) @PreAuthorize("hasAnyRole('SHOP','SHOP_STAFF')")
    public CustomFlowerOrderService.Results list(@PathVariable("shopId") String shopId,@AuthenticationPrincipal CurrentUser user,@RequestParam(name="status", required=false) CustomOrderStatus status,@RequestParam(name="page", defaultValue="0") int page){return service.shopRequests(shopId,user.id(),status,page);}
    @GetMapping({"/api/shop/mine/{shopId}/custom-orders/{id}","/api/staff/shops/{shopId}/custom-orders/{id}"}) @PreAuthorize("hasAnyRole('SHOP','SHOP_STAFF')")
    public CustomFlowerOrderService.Result shopDetail(@PathVariable("shopId") String shopId,@PathVariable("id") String id,@AuthenticationPrincipal CurrentUser user){return service.shopRequest(shopId,user.id(),id);}
    @PutMapping({"/api/shop/mine/{shopId}/custom-orders/{id}","/api/staff/shops/{shopId}/custom-orders/{id}"}) @PreAuthorize("hasAnyRole('SHOP','SHOP_STAFF')")
    public CustomFlowerOrderService.Result decide(@PathVariable("shopId") String shopId,@PathVariable("id") String id,@AuthenticationPrincipal CurrentUser user,@Valid @RequestBody CustomFlowerOrderService.Decision input){return service.decide(shopId,user.id(),id,input);}
    @PostMapping({"/api/shop/mine/{shopId}/custom-orders/{id}/quote","/api/staff/shops/{shopId}/custom-orders/{id}/quote"}) @PreAuthorize("hasAnyRole('SHOP','SHOP_STAFF')")
    public CustomFlowerOrderService.Result quote(@PathVariable("shopId") String shopId,@PathVariable("id") String id,@AuthenticationPrincipal CurrentUser user,@Valid @RequestBody CustomFlowerOrderService.Quote input){return service.quote(shopId,user.id(),id,input);}
    @PostMapping({"/api/shop/mine/{shopId}/custom-orders/{id}/reject","/api/staff/shops/{shopId}/custom-orders/{id}/reject"}) @PreAuthorize("hasAnyRole('SHOP','SHOP_STAFF')")
    public CustomFlowerOrderService.Result reject(@PathVariable("shopId") String shopId,@PathVariable("id") String id,@AuthenticationPrincipal CurrentUser user,@Valid @RequestBody CustomFlowerOrderService.Reason input){return service.reject(shopId,user.id(),id,input);}
}
