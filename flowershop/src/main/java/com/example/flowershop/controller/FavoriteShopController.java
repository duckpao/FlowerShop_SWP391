package com.example.flowershop.controller;

import com.example.flowershop.dto.auth.CurrentUser;
import com.example.flowershop.entity.User;
import com.example.flowershop.service.FavoriteShopService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer/favorite-shops")
@PreAuthorize("hasRole('CUSTOMER')")
@SecurityRequirement(name = "bearerAuth")
public class FavoriteShopController {

    private final FavoriteShopService favorites;

    public FavoriteShopController(
            FavoriteShopService favorites) {
        this.favorites = favorites;
    }

    /**
     * Get all favorite shops of current customer.
     *
     * GET /api/customer/favorite-shops
     */
    @GetMapping
    public List<FavoriteShopService.ShopFavoriteResponse> mine(
            @AuthenticationPrincipal CurrentUser user) {
        return favorites.mine(user.id());
    }

    /**
     * Check whether current customer follows a shop.
     *
     * GET /api/customer/favorite-shops/{shopId}
     */
    @GetMapping("/{shopId}")
    public FavoriteShopService.FavoriteStatus status(
            @AuthenticationPrincipal CurrentUser user,
            @PathVariable String shopId) {
        return favorites.status(
                user.id(),
                shopId);
    }

    /**
     * Follow a shop.
     *
     * PUT /api/customer/favorite-shops/{shopId}
     */
    @PutMapping("/{shopId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
    public void add(
            @AuthenticationPrincipal CurrentUser user,
            @PathVariable String shopId) {
        favorites.add(
                user.id(),
                shopId);
    }

    /**
     * Unfollow a shop.
     *
     * DELETE /api/customer/favorite-shops/{shopId}
     */
    @DeleteMapping("/{shopId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
    public void remove(
            @AuthenticationPrincipal CurrentUser user,
            @PathVariable String shopId) {
        favorites.remove(
                user.id(),
                shopId);
    }
}