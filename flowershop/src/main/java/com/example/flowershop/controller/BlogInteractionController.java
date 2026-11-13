package com.example.flowershop.controller;

import com.example.flowershop.dto.auth.CurrentUser;
import com.example.flowershop.entity.enums.InteractionType;
import com.example.flowershop.service.BlogInteractionService;
import com.example.flowershop.service.BlogService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@SecurityRequirement(name = "bearerAuth")
public class BlogInteractionController {
    private final BlogInteractionService service;

    public BlogInteractionController(BlogInteractionService service) {
        this.service = service;
    }

    @GetMapping("/api/public/blogs/{blogId}/engagement")
    public BlogInteractionService.EngagementResult engagement(@PathVariable(name = "blogId") String blogId) {
        return service.engagement(blogId);
    }

    @GetMapping("/api/public/blogs/{blogId}/comments")
    public BlogInteractionService.CommentResults comments(@PathVariable(name = "blogId") String blogId,
            @RequestParam(name = "page", defaultValue = "0") int page) {
        return service.comments(blogId, page);
    }

    @GetMapping("/api/customer/blogs/{blogId}/interactions")
    @PreAuthorize("hasRole('CUSTOMER')")
    public BlogInteractionService.InteractionResult mine(@PathVariable(name = "blogId") String blogId,
            @AuthenticationPrincipal CurrentUser user) {
        return service.mine(blogId, user.id());
    }

    @GetMapping("/api/customer/blogs/saved")
    @PreAuthorize("hasRole('CUSTOMER')")
    public BlogService.Results saved(
            @AuthenticationPrincipal CurrentUser user,
            @RequestParam(name = "page", defaultValue = "0") int page) {
        return service.saved(user.id(), page);
    }

    @PutMapping("/api/customer/blogs/{blogId}/likes")
    @PreAuthorize("hasRole('CUSTOMER')")
    public BlogInteractionService.InteractionResult like(@PathVariable(name = "blogId") String blogId,
            @AuthenticationPrincipal CurrentUser user) {
        return service.setInteraction(blogId, user.id(), InteractionType.LIKE, true);
    }

    @DeleteMapping("/api/customer/blogs/{blogId}/likes")
    @PreAuthorize("hasRole('CUSTOMER')")
    public BlogInteractionService.InteractionResult unlike(@PathVariable(name = "blogId") String blogId,
            @AuthenticationPrincipal CurrentUser user) {
        return service.setInteraction(blogId, user.id(), InteractionType.LIKE, false);
    }

    @PutMapping("/api/customer/blogs/{blogId}/saves")
    @PreAuthorize("hasRole('CUSTOMER')")
    public BlogInteractionService.InteractionResult save(@PathVariable(name = "blogId") String blogId,
            @AuthenticationPrincipal CurrentUser user) {
        return service.setInteraction(blogId, user.id(), InteractionType.SAVE, true);
    }

    @DeleteMapping("/api/customer/blogs/{blogId}/saves")
    @PreAuthorize("hasRole('CUSTOMER')")
    public BlogInteractionService.InteractionResult unsave(@PathVariable(name = "blogId") String blogId,
            @AuthenticationPrincipal CurrentUser user) {
        return service.setInteraction(blogId, user.id(), InteractionType.SAVE, false);
    }

    @PostMapping("/api/customer/blogs/{blogId}/comments")
    @PreAuthorize("hasRole('CUSTOMER')")
    @ResponseStatus(HttpStatus.CREATED)
    public BlogInteractionService.CommentResult addComment(@PathVariable(name = "blogId") String blogId,
            @AuthenticationPrincipal CurrentUser user,
            @Valid @RequestBody BlogInteractionService.CommentInput input) {
        return service.addComment(blogId, user.id(), input);
    }

    @DeleteMapping("/api/customer/blogs/{blogId}/comments/{commentId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable(name = "blogId") String blogId, @PathVariable(name = "commentId") String commentId,
            @AuthenticationPrincipal CurrentUser user) {
        service.deleteComment(blogId, commentId, user.id());
    }

    @PutMapping("/api/customer/blogs/{blogId}/comments/{commentId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public BlogInteractionService.CommentResult updateComment(@PathVariable(name = "blogId") String blogId,
            @PathVariable(name = "commentId") String commentId, @AuthenticationPrincipal CurrentUser user,
            @Valid @RequestBody BlogInteractionService.CommentInput input) {
        return service.updateComment(blogId, commentId, user.id(), input);
    }

    @GetMapping("/api/shop/mine/{shopId}/blogs/{blogId}/comments")
    @PreAuthorize("hasAnyRole('SHOP', 'SHOP_STAFF')")
    public BlogInteractionService.CommentResults managedComments(@PathVariable(name = "shopId") String shopId,
            @PathVariable(name = "blogId") String blogId, @AuthenticationPrincipal CurrentUser user,
            @RequestParam(name = "page", defaultValue = "0") int page) {
        return service.managedComments(shopId, blogId, user.id(), page);
    }

    @GetMapping("/api/shop/mine/{shopId}/blogs/{blogId}/engagement")
    @PreAuthorize("hasAnyRole('SHOP', 'SHOP_STAFF')")
    public BlogInteractionService.EngagementResult managedEngagement(@PathVariable(name = "shopId") String shopId,
            @PathVariable(name = "blogId") String blogId, @AuthenticationPrincipal CurrentUser user) {
        return service.managedEngagement(shopId, blogId, user.id());
    }

    @PostMapping("/api/shop/mine/{shopId}/blogs/{blogId}/comments/{commentId}/replies")
    @PreAuthorize("hasAnyRole('SHOP', 'SHOP_STAFF')")
    @ResponseStatus(HttpStatus.CREATED)
    public BlogInteractionService.CommentResult reply(@PathVariable(name = "shopId") String shopId,
            @PathVariable(name = "blogId") String blogId, @PathVariable(name = "commentId") String commentId,
            @AuthenticationPrincipal CurrentUser user, @Valid @RequestBody BlogInteractionService.CommentInput input) {
        return service.reply(shopId, blogId, commentId, user.id(), input);
    }

    @DeleteMapping("/api/shop/mine/{shopId}/blogs/{blogId}/comments/{commentId}")
    @PreAuthorize("hasAnyRole('SHOP', 'SHOP_STAFF')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void moderateComment(@PathVariable(name = "shopId") String shopId,
            @PathVariable(name = "blogId") String blogId, @PathVariable(name = "commentId") String commentId,
            @AuthenticationPrincipal CurrentUser user) {
        service.moderateComment(shopId, blogId, commentId, user.id());
    }
}
