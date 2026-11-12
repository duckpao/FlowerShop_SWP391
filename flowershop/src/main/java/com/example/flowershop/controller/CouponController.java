package com.example.flowershop.controller;

import com.example.flowershop.dto.auth.CurrentUser;
import com.example.flowershop.service.CouponService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class CouponController {
    private final CouponService service;
    public CouponController(CouponService service) { this.service=service; }
    @GetMapping("/api/admin/vouchers") @PreAuthorize("hasRole('ADMIN')")
    public List<CouponService.View> adminList(@AuthenticationPrincipal CurrentUser user) { return service.list(user.id(),null); }
    @PostMapping("/api/admin/vouchers") @PreAuthorize("hasRole('ADMIN')") @ResponseStatus(HttpStatus.CREATED)
    public CouponService.View adminCreate(@AuthenticationPrincipal CurrentUser user,@Valid @RequestBody CouponService.Input input) { return service.save(user.id(),null,null,input); }
    @PutMapping("/api/admin/vouchers/{id}") @PreAuthorize("hasRole('ADMIN')")
    public CouponService.View adminUpdate(@AuthenticationPrincipal CurrentUser user,@PathVariable("id") String id,@Valid @RequestBody CouponService.Input input) { return service.save(user.id(),null,id,input); }
    @DeleteMapping("/api/admin/vouchers/{id}") @PreAuthorize("hasRole('ADMIN')") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void adminDelete(@AuthenticationPrincipal CurrentUser user,@PathVariable("id") String id) { service.delete(user.id(),null,id); }
    @GetMapping("/api/shop/mine/{shopId}/vouchers") @PreAuthorize("hasRole('SHOP')")
    public List<CouponService.View> shopList(@AuthenticationPrincipal CurrentUser user,@PathVariable("shopId") String shopId) { return service.list(user.id(),shopId); }
    @PostMapping("/api/shop/mine/{shopId}/vouchers") @PreAuthorize("hasRole('SHOP')") @ResponseStatus(HttpStatus.CREATED)
    public CouponService.View shopCreate(@AuthenticationPrincipal CurrentUser user,@PathVariable("shopId") String shopId,@Valid @RequestBody CouponService.Input input) { return service.save(user.id(),shopId,null,input); }
    @PutMapping("/api/shop/mine/{shopId}/vouchers/{id}") @PreAuthorize("hasRole('SHOP')")
    public CouponService.View shopUpdate(@AuthenticationPrincipal CurrentUser user,@PathVariable("shopId") String shopId,@PathVariable("id") String id,@Valid @RequestBody CouponService.Input input) { return service.save(user.id(),shopId,id,input); }
    @DeleteMapping("/api/shop/mine/{shopId}/vouchers/{id}") @PreAuthorize("hasRole('SHOP')") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void shopDelete(@AuthenticationPrincipal CurrentUser user,@PathVariable("shopId") String shopId,@PathVariable("id") String id) { service.delete(user.id(),shopId,id); }
}
