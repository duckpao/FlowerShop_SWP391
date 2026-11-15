package com.example.flowershop.controller;

import com.example.flowershop.dto.auth.CurrentUser;
import com.example.flowershop.service.AdminReviewService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Admin liệt kê và xóa đánh giá sản phẩm vi phạm. */
@RestController
@RequestMapping("/api/admin/reviews")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
public class AdminReviewController {
    private final AdminReviewService service;

    public AdminReviewController(AdminReviewService service) {
        this.service = service;
    }

    @GetMapping
    public AdminReviewService.ReviewPage list(@RequestParam(name = "rating", required = false) Integer rating,
            @RequestParam(name = "productId", required = false) String productId,
            @RequestParam(name = "shopId", required = false) String shopId,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        return service.list(rating, productId, shopId, q, page, size);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
    public void delete(@PathVariable(name = "id") String id, @AuthenticationPrincipal CurrentUser admin,
            @Valid @RequestBody(required = false) AdminReviewService.DeleteInput body) {
        service.delete(id, admin.id(), body == null ? null : body.reason());
    }
}
