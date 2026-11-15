package com.example.flowershop.controller;
import com.example.flowershop.service.ManagerApplicationService;
import com.example.flowershop.dto.auth.CurrentUser;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import java.util.List;
@RestController @io.swagger.v3.oas.annotations.security.SecurityRequirement(name="bearerAuth")
public class ManagerApplicationController {
    private final ManagerApplicationService service;
    public ManagerApplicationController(ManagerApplicationService service) {this.service=service;}
    @PostMapping("/api/customer/manager-applications") @PreAuthorize("hasRole('CUSTOMER')")
    public ManagerApplicationService.Result submit(@AuthenticationPrincipal CurrentUser u,@Valid @RequestBody ManagerApplicationService.Input body) {return service.submit(u.id(),body);}
    @GetMapping("/api/account/manager-applications")
    public List<ManagerApplicationService.Result> mine(@AuthenticationPrincipal CurrentUser u) {return service.mine(u.id());}
    @GetMapping("/api/admin/shops/applications") @PreAuthorize("hasRole('ADMIN')")
    public ManagerApplicationService.Results list(@RequestParam(defaultValue="PENDING") String status,@RequestParam(defaultValue="0") int page) {return service.list(status,page);}
    @PutMapping("/api/admin/shops/applications/{id}") @PreAuthorize("hasRole('ADMIN')")
    public ManagerApplicationService.Result decide(@PathVariable String id,@AuthenticationPrincipal CurrentUser u,@Valid @RequestBody ManagerApplicationService.Decision body) {return service.decide(id,u.id(),body);}
}
