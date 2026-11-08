package com.example.flowershop.controller;
import com.example.flowershop.service.CatalogService;
import org.springframework.web.bind.annotation.*;

@RestController
public class PublicProductController {
    private final CatalogService catalog;
    public PublicProductController(CatalogService catalog) { this.catalog = catalog; }

    @GetMapping("/api/public/shops/{shopId}/products")
    public CatalogService.Results list(@PathVariable(name = "shopId") String shopId,
            @RequestParam(name = "page", defaultValue = "0") int page) { return catalog.published(shopId, page); }
}
