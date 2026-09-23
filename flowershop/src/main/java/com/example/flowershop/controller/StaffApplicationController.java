package com.example.flowershop.controller;
import com.example.flowershop.service.StaffApplicationService;
import com.example.flowershop.dto.auth.CurrentUser;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import java.util.List;
@RestController @io.swagger.v3.oas.annotations.security.SecurityRequirement(name="bearerAuth")
public class StaffApplicationController {
    private final StaffApplicationService service;
    public StaffApplicationController(StaffApplicationService service) {this.service=service;}
    @PostMapping("/api/customer/shops/{shopId}/applications") @PreAuthorize("hasRole('CUSTOMER')")
    public StaffApplicationService.Result apply(@PathVariable String shopId,@AuthenticationPrincipal CurrentUser user,@Valid @RequestBody StaffApplicationService.Input body) {return service.apply(shopId,user.id(),body);}
    @GetMapping("/api/account/staff-applications")
    public List<StaffApplicationService.Result> mine(@AuthenticationPrincipal CurrentUser user) {return service.mine(user.id());}
    @GetMapping("/api/shop/mine/{shopId}/applications") @PreAuthorize("hasRole('SHOP')")
    public List<StaffApplicationService.Result> list(@PathVariable String shopId,@AuthenticationPrincipal CurrentUser user) {return service.list(shopId,user.id());}
    public record Decision(@jakarta.validation.constraints.NotNull Boolean approve) {}
    @PutMapping("/api/shop/mine/{shopId}/applications/{id}") @PreAuthorize("hasRole('SHOP')")
    public StaffApplicationService.Result decide(@PathVariable String shopId,@PathVariable String id,@AuthenticationPrincipal CurrentUser user,@Valid @RequestBody Decision body) {return service.decide(shopId,id,user.id(),body.approve());}
}
