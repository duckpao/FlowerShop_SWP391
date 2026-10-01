package com.example.flowershop.controller;

import com.example.flowershop.service.CatalogService;
import com.example.flowershop.service.ProductReviewService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public")
public class PublicCatalogController {
    private final CatalogService catalog;
    private final ProductReviewService reviews;

    public PublicCatalogController(CatalogService catalog, ProductReviewService reviews) {
        this.catalog = catalog;
        this.reviews = reviews;
    }

    @GetMapping("/products/{id}/reviews")
    public ProductReviewService.Results reviews(@PathVariable String id,
            @RequestParam(defaultValue = "0") int page) { return reviews.list(id, page); }

    @GetMapping("/products")
    public CatalogService.Results browse(@RequestParam(defaultValue = "") String q,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String shopId,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return catalog.browse(q, categoryId, shopId, sort, page, size);
    }

    @GetMapping("/products/{id}")
    public CatalogService.ProductDetail detail(@PathVariable String id) { return catalog.detail(id); }

    @GetMapping("/categories")
    public List<CatalogService.CategoryCard> categories() { return catalog.categories(); }
}
