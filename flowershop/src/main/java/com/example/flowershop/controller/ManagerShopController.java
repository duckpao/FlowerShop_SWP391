package com.example.flowershop.controller;
import com.example.flowershop.dto.account.ShopResponse;
import com.example.flowershop.dto.auth.CurrentUser;
import com.example.flowershop.service.ManagerShopService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import java.util.List;

@RestController @RequestMapping("/api/shop/mine") @PreAuthorize("hasRole('SHOP')") @SecurityRequirement(name="bearerAuth")
public class ManagerShopController {
    private final ManagerShopService service;
    public ManagerShopController(ManagerShopService service) { this.service=service; }
    @GetMapping public List<ShopResponse> mine(@AuthenticationPrincipal CurrentUser u) { return service.mine(u.id()); }
    @GetMapping("/{id}/address") public List<com.example.flowershop.dto.account.AddressResponse> address(@PathVariable String id,@AuthenticationPrincipal CurrentUser u) { return service.address(id,u.id()); }
    @PutMapping("/{id}/address") @Parameter(name="X-CSRF-TOKEN",in=ParameterIn.HEADER,required=true)
    public com.example.flowershop.dto.account.AddressResponse saveAddress(@PathVariable String id,@AuthenticationPrincipal CurrentUser u,@Valid @RequestBody com.example.flowershop.dto.account.AddressRequest body) { return service.saveAddress(id,u.id(),body); }
    @PutMapping("/{id}") @Parameter(name="X-CSRF-TOKEN",in=ParameterIn.HEADER,required=true)
    public ShopResponse update(@PathVariable String id,@AuthenticationPrincipal CurrentUser u,@Valid @RequestBody ManagerShopService.Profile body) { return service.update(id,u.id(),body); }
    @GetMapping("/{id}/staff") public List<ManagerShopService.StaffResponse> staff(@PathVariable String id,@AuthenticationPrincipal CurrentUser u) { return service.staff(id,u.id()); }
    @PostMapping("/{id}/staff") @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    @Parameter(name="X-CSRF-TOKEN",in=ParameterIn.HEADER,required=true)
    public ManagerShopService.StaffResponse add(@PathVariable String id,@AuthenticationPrincipal CurrentUser u,@Valid @RequestBody ManagerShopService.StaffInput body) { return service.add(id,u.id(),body.email()); }
    @PutMapping("/{id}/staff/{userId}") @Parameter(name="X-CSRF-TOKEN",in=ParameterIn.HEADER,required=true)
    public ManagerShopService.StaffResponse active(@PathVariable String id,@PathVariable String userId,@AuthenticationPrincipal CurrentUser u,@Valid @RequestBody ManagerShopService.ActiveInput body) { return service.active(id,u.id(),userId,body.active()); }
}
