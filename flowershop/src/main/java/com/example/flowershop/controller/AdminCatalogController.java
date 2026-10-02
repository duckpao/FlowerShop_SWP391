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

// API quản lý danh mục và kiểm duyệt sản phẩm cho ADMIN; hiện chỉ có xem/ẩn/bỏ ẩn sản phẩm, không có CRUD đầy đủ.
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
public class AdminCatalogController {
    private final AdminCatalogService service;

    public AdminCatalogController(AdminCatalogService service) { this.service = service; }

    @GetMapping("/categories")
    // GET /categories -> đọc cả ACTIVE và INACTIVE cho màn hình quản trị.
    public List<AdminCatalogService.CategoryResult> categories() { return service.categories(); }

    @PostMapping("/categories") @ResponseStatus(HttpStatus.CREATED)
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
    // POST danh mục -> createCategory, gắn createdBy bằng id Admin.
    public AdminCatalogService.CategoryResult create(@AuthenticationPrincipal CurrentUser admin,
            @Valid @RequestBody AdminCatalogService.CategoryInput body) {
        return service.createCategory(admin.id(), body);
    }

    @PutMapping("/categories/{id}")
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
    // PUT danh mục -> updateCategory; INACTIVE dùng để ngừng chọn cho sản phẩm mới.
    public AdminCatalogService.CategoryResult update(@PathVariable String id,
            @AuthenticationPrincipal CurrentUser admin,
            @Valid @RequestBody AdminCatalogService.CategoryInput body) {
        return service.updateCategory(id, admin.id(), body);
    }

    @GetMapping("/products")
    // GET /products -> danh sách toàn sàn có lọc, bao gồm sản phẩm bị ẩn.
    public CatalogService.Results products(@RequestParam(defaultValue = "") String q,
            @RequestParam(required = false) String shopId,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "0") int page) {
        return service.products(q, shopId, status, page);
    }

    @PutMapping("/products/{id}/hidden")
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
    // PUT /products/{id}/hidden -> setHidden: thay cờ adminHidden, không sửa status của Shop.
    public ProductCardAssembler.ProductCard hide(@PathVariable String id,
            @AuthenticationPrincipal CurrentUser admin,
            @Valid @RequestBody AdminCatalogService.HiddenInput body) {
        return service.setHidden(id, body.hidden(), admin.id());
    }
}
