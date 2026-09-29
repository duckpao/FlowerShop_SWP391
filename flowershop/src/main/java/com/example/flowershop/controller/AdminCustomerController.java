package com.example.flowershop.controller;

import com.example.flowershop.dto.account.CustomerResponse;
import com.example.flowershop.dto.auth.CurrentUser;
import com.example.flowershop.service.AdminCustomerService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
// Do Van Quang
@RestController
@RequestMapping("/api/admin/customers")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name="bearerAuth")
public class AdminCustomerController {
    private final AdminCustomerService customers;
    public AdminCustomerController(AdminCustomerService customers) { this.customers=customers; }
    public record BlockRequest(@NotNull(message="Cần chỉ định blocked là true hoặc false") Boolean blocked) {}
    @GetMapping
    public AdminCustomerService.CustomerPage list(@RequestParam(defaultValue="") String q,
            @RequestParam(defaultValue="") String status,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="10") int size) {
        return customers.list(q,status,page,size);
    }
    @GetMapping("/{id}")
    public CustomerResponse detail(@PathVariable String id) { return customers.detail(id); }
    @PutMapping("/{id}/blocked")
    @Parameter(name="X-CSRF-TOKEN",in=ParameterIn.HEADER,required=true)
    public CustomerResponse block(@PathVariable String id,@Valid @RequestBody BlockRequest request,
                                  @AuthenticationPrincipal CurrentUser admin) {
        return customers.setBlocked(id,request.blocked(),admin.id());
    }
}
