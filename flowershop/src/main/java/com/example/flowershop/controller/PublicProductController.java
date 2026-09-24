package com.example.flowershop.controller;
import com.example.flowershop.service.ManagerProductService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
public class PublicProductController {
    private final ManagerProductService service;
    public PublicProductController(ManagerProductService service) {this.service=service;}
    @GetMapping("/api/public/shops/{shopId}/products")
    public ManagerProductService.Results list(@PathVariable String shopId,@RequestParam(defaultValue="0") int page) {return service.published(shopId,page);}
    @GetMapping("/api/public/products")
    public ManagerProductService.CatalogResults catalog(@RequestParam(required=false) String categoryId,@RequestParam(required=false) String q,@RequestParam(defaultValue="0") int page) {
        return service.catalog(categoryId,q,page);
    }
    @GetMapping("/api/public/categories")
    public List<ManagerProductService.CategoryOption> categories() {return service.categories();}
    @GetMapping("/api/public/products/{id}")
    public ManagerProductService.Detail detail(@PathVariable String id) {return service.detail(id);}
}
