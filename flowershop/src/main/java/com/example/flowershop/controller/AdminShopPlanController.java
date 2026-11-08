package com.example.flowershop.controller;
import com.example.flowershop.dto.auth.CurrentUser;
import com.example.flowershop.service.ShopPlanService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.util.List;
@RestController @PreAuthorize("hasRole('ADMIN')")
public class AdminShopPlanController {
    private final ShopPlanService plans;
    public AdminShopPlanController(ShopPlanService plans){this.plans=plans;}
    @GetMapping("/api/admin/shop-plans")
    public List<ShopPlanService.Plan> plans(@AuthenticationPrincipal CurrentUser admin){return plans.adminPlans(admin.id());}
    @GetMapping("/api/admin/shops/{shopId}/subscription")
    public ShopPlanService.AdminStatus detail(@PathVariable("shopId") String shopId,@AuthenticationPrincipal CurrentUser admin){return plans.adminStatus(shopId,admin.id());}
    @PostMapping("/api/admin/shops/{shopId}/subscription/activate")
    public ShopPlanService.AdminStatus activate(@PathVariable("shopId") String shopId,@AuthenticationPrincipal CurrentUser admin,@Valid @RequestBody ShopPlanService.AdminGrant body){return plans.adminGrant(shopId,admin.id(),body);}
    @PostMapping("/api/admin/shops/{shopId}/subscription/payments/{invoice}/confirm")
    public ShopPlanService.AdminStatus confirm(@PathVariable("shopId") String shopId,@PathVariable("invoice") String invoice,@AuthenticationPrincipal CurrentUser admin,@Valid @RequestBody ShopPlanService.AdminConfirmation body){return plans.adminConfirm(shopId,admin.id(),invoice,body);}
}
