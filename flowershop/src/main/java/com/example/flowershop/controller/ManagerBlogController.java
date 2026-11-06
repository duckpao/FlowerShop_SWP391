package com.example.flowershop.controller;

import com.example.flowershop.dto.auth.CurrentUser;
import com.example.flowershop.service.BlogService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/shop/mine/{shopId}/blogs")
@PreAuthorize("hasRole('SHOP')")
@SecurityRequirement(name = "bearerAuth")
public class ManagerBlogController {
    private final BlogService service;

    public ManagerBlogController(BlogService service) {
        this.service = service;
    }

    @GetMapping
    public BlogService.Results list(@PathVariable String shopId, @AuthenticationPrincipal CurrentUser user,
            @RequestParam(defaultValue = "0") int page) {
        return service.listManaged(shopId, user.id(), page);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BlogService.Result create(@PathVariable String shopId, @AuthenticationPrincipal CurrentUser user,
            @Valid @RequestPart("blog") BlogService.Input input,
            @RequestParam(required = false) List<MultipartFile> images) {
        return service.save(shopId, null, user.id(), input, images);
    }

    @PutMapping("/{id}")
    public BlogService.Result update(@PathVariable String shopId, @PathVariable String id,
            @AuthenticationPrincipal CurrentUser user, @Valid @RequestPart("blog") BlogService.Input input,
            @RequestParam(required = false) List<MultipartFile> images) {
        return service.save(shopId, id, user.id(), input, images);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void hide(@PathVariable String shopId, @PathVariable String id,
            @AuthenticationPrincipal CurrentUser user) {
        service.hide(shopId, id, user.id());
    }
}
