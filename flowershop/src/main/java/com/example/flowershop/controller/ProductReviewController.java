package com.example.flowershop.controller;

import com.example.flowershop.dto.auth.CurrentUser;
import com.example.flowershop.service.ProductReviewService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@SecurityRequirement(name = "bearerAuth")
public class ProductReviewController {
    private final ProductReviewService reviews;

    public ProductReviewController(ProductReviewService reviews) { this.reviews = reviews; }

    @GetMapping("/api/customer/products/{id}/reviews")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ProductReviewService.ReviewResult mine(@PathVariable String id,
            @AuthenticationPrincipal CurrentUser user) {
        return reviews.mine(id, user.id());
    }

    @PostMapping("/api/customer/products/{id}/reviews")
    @PreAuthorize("hasRole('CUSTOMER')") @ResponseStatus(HttpStatus.CREATED)
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
    public ProductReviewService.ReviewResult write(@PathVariable String id,
            @AuthenticationPrincipal CurrentUser user,
            @Valid @RequestBody ProductReviewService.ReviewInput body) {
        return reviews.write(id, user.id(), body);
    }

    @PutMapping("/api/customer/products/{id}/reviews")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
    public ProductReviewService.ReviewResult update(@PathVariable String id,
            @AuthenticationPrincipal CurrentUser user,
            @Valid @RequestBody ProductReviewService.ReviewInput body) {
        return reviews.update(id, user.id(), body);
    }

    @DeleteMapping("/api/customer/products/{id}/reviews")
    @PreAuthorize("hasRole('CUSTOMER')") @ResponseStatus(HttpStatus.NO_CONTENT)
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
    public void delete(@PathVariable String id, @AuthenticationPrincipal CurrentUser user) {
        reviews.delete(id, user.id());
    }

    @PutMapping("/api/shop/mine/{shopId}/reviews/{reviewId}/reply")
    @PreAuthorize("hasRole('SHOP')")
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
    public ProductReviewService.ReviewResult reply(@PathVariable String shopId,
            @PathVariable String reviewId, @AuthenticationPrincipal CurrentUser user,
            @Valid @RequestBody ProductReviewService.ReplyInput body) {
        return reviews.reply(shopId, reviewId, user.id(), body.reply());
    }
}
