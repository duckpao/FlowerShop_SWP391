package com.example.flowershop.controller;
import com.example.flowershop.service.ManagerProductService;
import com.example.flowershop.dto.auth.CurrentUser;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import java.util.List;
@RestController @RequestMapping("/api/shop/mine/{shopId}/products") @PreAuthorize("hasRole('SHOP')")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name="bearerAuth")
public class ManagerProductController {
    private final ManagerProductService service;
    public ManagerProductController(ManagerProductService s) {service=s;}
    @GetMapping public ManagerProductService.Results list(@PathVariable String shopId,@AuthenticationPrincipal CurrentUser u,@RequestParam(defaultValue="0") int page) {return service.list(shopId,u.id(),page);}
    @GetMapping("/categories") public List<ManagerProductService.CategoryOption> categories(@PathVariable String shopId,@AuthenticationPrincipal CurrentUser u) {service.list(shopId,u.id(),0);return service.categories();}
    @PostMapping @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public ManagerProductService.Result create(@PathVariable String shopId,@AuthenticationPrincipal CurrentUser u,@Valid @RequestBody ManagerProductService.Input body) {return service.save(shopId,null,u.id(),body);}
    @PutMapping("/{id}") public ManagerProductService.Result update(@PathVariable String shopId,@PathVariable String id,@AuthenticationPrincipal CurrentUser u,@Valid @RequestBody ManagerProductService.Input body) {return service.save(shopId,id,u.id(),body);}
    @DeleteMapping("/{id}") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void hide(@PathVariable String shopId,@PathVariable String id,@AuthenticationPrincipal CurrentUser u) {service.hide(shopId,id,u.id());}
}
