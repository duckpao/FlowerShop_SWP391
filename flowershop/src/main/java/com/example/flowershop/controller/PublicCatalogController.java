package com.example.flowershop.controller;

import com.example.flowershop.service.CatalogService;
import com.example.flowershop.service.ProductReviewService;
import com.example.flowershop.entity.enums.ProductType;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

// Cửa vào HTTP cho Product List/Details/Category và đọc review. Spring lấy query/path -> gọi service -> chuyển record trả về thành JSON.
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
    // GET /products/{id}/reviews -> ProductReviewService.list, mặc định 10 review/trang ở service.
    public ProductReviewService.Results reviews(@PathVariable String id,
            @RequestParam(defaultValue = "0") int page) { return reviews.list(id, page); }

    @GetMapping("/products")
    // GET /products: đọc bộ lọc từ query string; mặc định page=0, size=12 -> CatalogService.browse.
    public CatalogService.Results browse(@RequestParam(defaultValue = "") String q,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String shopId,
            @RequestParam(required = false) ProductType type,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return catalog.browse(q, categoryId, shopId, type, minPrice, maxPrice, sort, page, size);
    }

    @GetMapping("/products/{id}")
    // GET /products/{id} -> chi tiết, danh sách URL ảnh và thống kê review.
    public CatalogService.ProductDetail detail(@PathVariable String id) { return catalog.detail(id); }

    @GetMapping("/categories")
    // GET /categories -> các danh mục ACTIVE kèm số sản phẩm công khai trong mỗi danh mục.
    public List<CatalogService.CategoryCard> categories() { return catalog.categories(); }
}
