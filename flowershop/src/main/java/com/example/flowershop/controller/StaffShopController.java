package com.example.flowershop.controller;
import com.example.flowershop.dto.account.ShopResponse;
import com.example.flowershop.dto.auth.CurrentUser;
import com.example.flowershop.service.ManagerShopService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
@RestController @PreAuthorize("hasRole('SHOP_STAFF')") @SecurityRequirement(name="bearerAuth")
public class StaffShopController {
    private final ManagerShopService service;
    public StaffShopController(ManagerShopService service) { this.service=service; }
    @GetMapping("/api/staff/shops") public List<ShopResponse> shops(@AuthenticationPrincipal CurrentUser u) { return service.assigned(u.id()); }
}
