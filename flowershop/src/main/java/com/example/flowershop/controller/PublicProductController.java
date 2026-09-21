package com.example.flowershop.controller;
import com.example.flowershop.service.ManagerProductService;
import org.springframework.web.bind.annotation.*;
@RestController
public class PublicProductController {
    private final ManagerProductService service;
    public PublicProductController(ManagerProductService service) {this.service=service;}
    @GetMapping("/api/public/shops/{shopId}/products")
    public ManagerProductService.Results list(@PathVariable String shopId,@RequestParam(defaultValue="0") int page) {return service.published(shopId,page);}
}
