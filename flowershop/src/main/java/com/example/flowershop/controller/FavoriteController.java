package com.example.flowershop.controller;

import com.example.flowershop.dto.auth.CurrentUser;
import com.example.flowershop.service.CatalogService;
import com.example.flowershop.service.FavoriteService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// API Favorite/View Favorite dành cho CUSTOMER. user.id() lấy từ token đã xác thực, không lấy userId do FE gửi.
@RestController
@RequestMapping("/api/customer/favorites")
@PreAuthorize("hasRole('CUSTOMER')")
@SecurityRequirement(name = "bearerAuth")
public class FavoriteController {
    private final FavoriteService favorites;

    public FavoriteController(FavoriteService favorites) { this.favorites = favorites; }

    @GetMapping
    // GET ?page -> favorites.mine(user.id(), page) -> JSON danh sách đã lưu của chính người gọi.
    public CatalogService.Results mine(@AuthenticationPrincipal CurrentUser user,
            @RequestParam(defaultValue = "0") int page) {
        return favorites.mine(user.id(), page);
    }

    @PutMapping("/{productId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
    // PUT /{productId} -> favorites.add; trả 204, FE cập nhật nút tim sau thành công.
    public void add(@AuthenticationPrincipal CurrentUser user, @PathVariable String productId) {
        favorites.add(user.id(), productId);
    }

    @DeleteMapping("/{productId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
    // DELETE /{productId} -> favorites.remove; chỉ bỏ quan hệ yêu thích, không xóa sản phẩm.
    public void remove(@AuthenticationPrincipal CurrentUser user, @PathVariable String productId) {
        favorites.remove(user.id(), productId);
    }
}
