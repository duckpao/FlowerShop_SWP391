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
    @PostMapping("/api/admin/shop-plans")
    public ShopPlanService.Plan create(@AuthenticationPrincipal CurrentUser admin,@Valid @RequestBody ShopPlanService.PlanInput input){return plans.savePlan(admin.id(),null,input);}
    @PutMapping("/api/admin/shop-plans/{id}")
    public ShopPlanService.Plan update(@AuthenticationPrincipal CurrentUser admin,@PathVariable("id") String id,@Valid @RequestBody ShopPlanService.PlanInput input){return plans.savePlan(admin.id(),id,input);}
    @DeleteMapping("/api/admin/shop-plans/{id}")
    public void remove(@AuthenticationPrincipal CurrentUser admin,@PathVariable("id") String id){plans.removePlan(admin.id(),id);}
    @GetMapping("/api/admin/shops/{shopId}/subscription/products")
    public List<ShopPlanService.ManagedProduct> products(@AuthenticationPrincipal CurrentUser admin,@PathVariable("shopId") String shopId){return plans.adminProducts(shopId,admin.id());}
    @PutMapping("/api/admin/shops/{shopId}/subscription/featured/{productId}")
    public void feature(@AuthenticationPrincipal CurrentUser admin,@PathVariable("shopId") String shopId,@PathVariable("productId") String productId,@Valid @RequestBody ShopPlanService.Feature input){plans.adminFeature(shopId,admin.id(),productId,input.featured());}
    @GetMapping("/api/admin/shops/{shopId}/subscription")
    public ShopPlanService.AdminStatus detail(@PathVariable("shopId") String shopId,@AuthenticationPrincipal CurrentUser admin){return plans.adminStatus(shopId,admin.id());}
    @PostMapping("/api/admin/shops/{shopId}/subscription/activate")
    public ShopPlanService.AdminStatus activate(@PathVariable("shopId") String shopId,@AuthenticationPrincipal CurrentUser admin,@Valid @RequestBody ShopPlanService.AdminGrant body){return plans.adminGrant(shopId,admin.id(),body);}
    @PostMapping("/api/admin/shops/{shopId}/subscription/payments/{invoice}/confirm")
    public ShopPlanService.AdminStatus confirm(@PathVariable("shopId") String shopId,@PathVariable("invoice") String invoice,@AuthenticationPrincipal CurrentUser admin,@Valid @RequestBody ShopPlanService.AdminConfirmation body){return plans.adminConfirm(shopId,admin.id(),invoice,body);}
}
