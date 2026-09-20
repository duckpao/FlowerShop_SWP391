package com.example.flowershop.controller;

import com.example.flowershop.dto.account.ShopResponse;
import com.example.flowershop.dto.auth.CurrentUser;
import com.example.flowershop.service.AdminShopService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/shops")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name="bearerAuth")
public class AdminShopController {
    private final AdminShopService shops;
    public AdminShopController(AdminShopService shops) { this.shops=shops; }
    @GetMapping
    public AdminShopService.ShopPage list(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="") String status,
                                        @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="10") int size) {
        return shops.list(q,status,page,size);
    }
    @GetMapping("/{id}") public ShopResponse detail(@PathVariable String id) { return shops.detail(id); }
    @PutMapping("/{id}/{action:approve|block|unblock}")
    @Parameter(name="X-CSRF-TOKEN",in=ParameterIn.HEADER,required=true)
    public ShopResponse transition(@PathVariable String id,@PathVariable String action,@AuthenticationPrincipal CurrentUser admin) {
        return shops.transition(id,action,admin.id());
    }
}
