package com.example.flowershop.controller;
import com.example.flowershop.repository.ShopRepository;
import com.example.flowershop.entity.enums.ShopStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.PageRequest;
import java.util.List;
@RestController @RequestMapping("/api/public/shops")
public class PublicShopController {
    private final ShopRepository shops;
    public PublicShopController(ShopRepository shops) {this.shops=shops;}
    public record ShopCard(String id,String name,String description) {}
    @GetMapping("/{id}") public ShopCard detail(@PathVariable String id) {
        var shop=shops.findById(id).filter(s->s.getStatus()==ShopStatus.ACTIVE).orElseThrow(()->new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Không tìm thấy shop đang hoạt động."));
        return new ShopCard(shop.getId(),shop.getName(),shop.getDescription());
    }
    // catalogService.shops dùng kết quả này cho ô lọc Cửa hàng; ở đây controller gọi repository trực tiếp (tối đa 50 shop).
    @GetMapping public List<ShopCard> search(@RequestParam(defaultValue="") String q) {
        String term=q.strip(); if(term.length()>100) throw new IllegalArgumentException("Từ khóa tối đa 100 ký tự.");
        return shops.findByStatusAndNameContainingIgnoreCaseOrderByNameAsc(ShopStatus.ACTIVE,term,PageRequest.of(0,50)).stream().map(s->new ShopCard(s.getId(),s.getName(),s.getDescription())).toList();
    }
}
