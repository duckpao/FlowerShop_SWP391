package com.example.flowershop.controller;

import com.example.flowershop.dto.account.*;
import com.example.flowershop.dto.auth.CurrentUser;
import com.example.flowershop.service.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/account")
@SecurityRequirement(name="bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN','CUSTOMER','SHOP','SHOP_STAFF')")
public class AccountController {
    private final AccountService accounts;
    private final DeliveryAreaService areas;
    public AccountController(AccountService accounts, DeliveryAreaService areas) { this.accounts=accounts; this.areas=areas; }
    @GetMapping("/profile")
    public ProfileResponse profile(@AuthenticationPrincipal CurrentUser user) { return accounts.profile(user.id()); }
    @PutMapping("/profile")
    @Parameter(name="X-CSRF-TOKEN",in=ParameterIn.HEADER,required=true)
    public ProfileResponse update(@AuthenticationPrincipal CurrentUser user, @Valid @RequestBody ProfileRequest request) {
        return accounts.updateProfile(user.id(),request);
    }
    @GetMapping("/delivery-areas") @PreAuthorize("hasRole('CUSTOMER')")
    public List<String> areas() { return areas.list(); }
    @GetMapping("/addresses") @PreAuthorize("hasRole('CUSTOMER')")
    public List<AddressResponse> addresses(@AuthenticationPrincipal CurrentUser user) { return accounts.list(user.id()); }
    @PostMapping("/addresses") @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    @PreAuthorize("hasRole('CUSTOMER')")
    @Parameter(name="X-CSRF-TOKEN",in=ParameterIn.HEADER,required=true)
    public AddressResponse add(@AuthenticationPrincipal CurrentUser user,@Valid @RequestBody AddressRequest request) {
        return accounts.save(user.id(),null,request);
    }
    @PutMapping("/addresses/{id}") @PreAuthorize("hasRole('CUSTOMER')")
    @Parameter(name="X-CSRF-TOKEN",in=ParameterIn.HEADER,required=true)
    public AddressResponse updateAddress(@AuthenticationPrincipal CurrentUser user,@PathVariable String id,@Valid @RequestBody AddressRequest request) {
        return accounts.save(user.id(),id,request);
    }
    @PutMapping("/addresses/{id}/default") @PreAuthorize("hasRole('CUSTOMER')")
    @Parameter(name="X-CSRF-TOKEN",in=ParameterIn.HEADER,required=true)
    public AddressResponse setDefault(@AuthenticationPrincipal CurrentUser user,@PathVariable String id) {
        return accounts.setDefault(user.id(),id);
    }
    @DeleteMapping("/addresses/{id}") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('CUSTOMER')")
    @Parameter(name="X-CSRF-TOKEN",in=ParameterIn.HEADER,required=true)
    public void delete(@AuthenticationPrincipal CurrentUser user,@PathVariable String id) { accounts.delete(user.id(),id); }
}
