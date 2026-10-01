package com.example.flowershop.controller;

import com.example.flowershop.dto.auth.CurrentUser;
import com.example.flowershop.service.AdminCatalogService;
import com.example.flowershop.service.CatalogService;
import com.example.flowershop.service.ProductCardAssembler;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
public class AdminCatalogController {
    private final AdminCatalogService service;

    public AdminCatalogController(AdminCatalogService service) { this.service = service; }

    @GetMapping("/categories")
    public List<AdminCatalogService.CategoryResult> categories() { return service.categories(); }

    @PostMapping("/categories") @ResponseStatus(HttpStatus.CREATED)
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
    public AdminCatalogService.CategoryResult create(@AuthenticationPrincipal CurrentUser admin,
            @Valid @RequestBody AdminCatalogService.CategoryInput body) {
        return service.createCategory(admin.id(), body);
    }

    @PutMapping("/categories/{id}")
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
    public AdminCatalogService.CategoryResult update(@PathVariable String id,
            @AuthenticationPrincipal CurrentUser admin,
            @Valid @RequestBody AdminCatalogService.CategoryInput body) {
        return service.updateCategory(id, admin.id(), body);
    }

    @GetMapping("/products")
    public CatalogService.Results products(@RequestParam(defaultValue = "") String q,
            @RequestParam(required = false) String shopId,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "0") int page) {
        return service.products(q, shopId, status, page);
    }

    @PutMapping("/products/{id}/hidden")
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
    public ProductCardAssembler.ProductCard hide(@PathVariable String id,
            @AuthenticationPrincipal CurrentUser admin,
            @Valid @RequestBody AdminCatalogService.HiddenInput body) {
        return service.setHidden(id, body.hidden(), admin.id());
    }
}
