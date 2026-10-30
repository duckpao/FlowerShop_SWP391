package com.example.flowershop.controller;

import com.example.flowershop.service.CatalogService;
import com.example.flowershop.service.ProductReviewService;
import com.example.flowershop.entity.enums.ProductType;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
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
    public ProductReviewService.Results reviews(@PathVariable(name = "id") String id,
            @RequestParam(name = "page", defaultValue = "0") int page) { return reviews.list(id, page); }

    @GetMapping("/products")
    public CatalogService.Results browse(@RequestParam(name = "q", defaultValue = "") String q,
            @RequestParam(name = "categoryId", required = false) String categoryId,
            @RequestParam(name = "shopId", required = false) String shopId,
            @RequestParam(name = "type", required = false) ProductType type,
            @RequestParam(name = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(name = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(name = "sort", defaultValue = "newest") String sort,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "12") int size) {
        return catalog.browse(q, categoryId, shopId, type, minPrice, maxPrice, sort, page, size);
    }

    @GetMapping("/products/{id}")
    public CatalogService.ProductDetail detail(@PathVariable(name = "id") String id) { return catalog.detail(id); }

    @GetMapping("/categories")
    public List<CatalogService.CategoryCard> categories() { return catalog.categories(); }
}
