package com.example.flowershop.controller;

import com.example.flowershop.service.BlogService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/blogs")
public class PublicBlogController {
    private final BlogService service;

    public PublicBlogController(BlogService service) {
        this.service = service;
    }

    @GetMapping
    public BlogService.Results list(@RequestParam(required = false) String shopId,
            @RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "0") int page) {
        return service.listPublic(shopId, q, page);
    }

    @GetMapping("/{id}")
    public BlogService.Result detail(@PathVariable String id) {
        return service.publicDetail(id);
    }
}
