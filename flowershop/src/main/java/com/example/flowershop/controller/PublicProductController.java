package com.example.flowershop.controller;
import com.example.flowershop.service.CatalogService;
import org.springframework.web.bind.annotation.*;

// GET sản phẩm của một shop từ ProductsView manage=false -> list -> CatalogService.published -> browse.
@RestController
public class PublicProductController {
    private final CatalogService catalog;
    public PublicProductController(CatalogService catalog) { this.catalog = catalog; }

    @GetMapping("/api/public/shops/{shopId}/products")
    public CatalogService.Results list(@PathVariable String shopId,
            @RequestParam(defaultValue = "0") int page) { return catalog.published(shopId, page); }
}
