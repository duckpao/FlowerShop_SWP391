package com.example.flowershop.controller;
import com.example.flowershop.service.ShopPlanService;
import com.example.flowershop.service.ProductCardAssembler;
import com.example.flowershop.dto.auth.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.util.List;
@RestController
public class ShopPlanController {
    private final ShopPlanService plans;
    public ShopPlanController(ShopPlanService plans){this.plans=plans;}
    @GetMapping("/api/shop/plans") @PreAuthorize("hasRole('SHOP')")
    public List<ShopPlanService.Plan> plans(){return plans.plans();}
    @GetMapping("/api/shop/mine/{shopId}/subscription") @PreAuthorize("hasRole('SHOP')")
    public ShopPlanService.Status status(@PathVariable("shopId") String shopId,@AuthenticationPrincipal CurrentUser u){return plans.status(shopId,u.id());}
    @PostMapping("/api/shop/mine/{shopId}/subscription/checkout") @PreAuthorize("hasRole('SHOP')")
    public ShopPlanService.Checkout checkout(@PathVariable("shopId") String shopId,@AuthenticationPrincipal CurrentUser u,@Valid @RequestBody ShopPlanService.Purchase input){return plans.checkout(shopId,u.id(),input.planId());}
    @GetMapping("/api/shop/mine/{shopId}/subscription/payments/{invoice}") @PreAuthorize("hasRole('SHOP')")
    public ShopPlanService.PaymentStatus payment(@PathVariable("shopId") String shopId,@PathVariable("invoice") String invoice,@AuthenticationPrincipal CurrentUser u){return plans.payment(shopId,u.id(),invoice);}
    @PutMapping("/api/shop/mine/{shopId}/subscription/featured/{productId}") @PreAuthorize("hasRole('SHOP')")
    public void featured(@PathVariable("shopId") String shopId,@PathVariable("productId") String productId,@AuthenticationPrincipal CurrentUser u,@Valid @RequestBody ShopPlanService.Feature input){plans.feature(shopId,u.id(),productId,input.featured());}
    @GetMapping("/api/public/featured-products")
    public List<ProductCardAssembler.ProductCard> featured(){return plans.featured();}
}
