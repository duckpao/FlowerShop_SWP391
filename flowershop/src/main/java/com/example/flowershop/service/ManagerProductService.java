package com.example.flowershop.service;
import com.example.flowershop.entity.*;
import com.example.flowershop.entity.enums.*;
import com.example.flowershop.repository.*;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.*;
@Service @Transactional(readOnly=true)
public class ManagerProductService {
    public record Input(@NotBlank @Size(max=255) String name,@NotNull @Size(max=5000) String description,
        @NotBlank @Size(max=36) String categoryId,@NotNull @DecimalMin("0.01") @Digits(integer=10,fraction=2) BigDecimal price,
        @NotNull @Min(0) @Max(1000000) Integer stock,@NotNull ProductStatus status) {}
    public record Result(String id,String shopId,String categoryId,String categoryName,String name,String description,BigDecimal price,Integer stock,ProductStatus status) {}
    public record Results(List<Result> content,int page,int totalPages,long totalElements) {}
    public record CategoryOption(String id,String name) {}
    private final ShopRepository shops;private final ProductRepository products;private final CategoryRepository categories;
    public ManagerProductService(ShopRepository s,ProductRepository p,CategoryRepository c) {shops=s;products=p;categories=c;}
    private static ResponseStatusException missing() {return new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy shop hoặc sản phẩm.");}
    private Shop owned(String id,String actor,boolean write) {
        var shop=(write?shops.findForUpdate(id):shops.findById(id)).orElseThrow(ManagerProductService::missing);
        if(!shop.getOwner().getId().equals(actor)) throw missing();
        if(write && (shop.getStatus()!=ShopStatus.ACTIVE || shop.getOwner().getRole()!=UserRole.SHOP || shop.getOwner().getStatus()!=UserStatus.ACTIVE)) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Shop phải được duyệt và đang hoạt động để quản lý mặt hàng.");
        return shop;
    }
    private Result result(Product p) {return new Result(p.getId(),p.getShop().getId(),p.getCategory().getId(),p.getCategory().getName(),p.getName(),p.getDescription(),p.getPrice(),p.getStock(),p.getStatus());}
    private Pageable page(int page) {if(page<0 || page>100000) throw new IllegalArgumentException("Trang không hợp lệ.");return PageRequest.of(page,20,Sort.by(Sort.Order.desc("createdDate"),Sort.Order.asc("id")));}
    private Results response(Page<Product> p) {return new Results(p.getContent().stream().map(this::result).toList(),p.getNumber(),p.getTotalPages(),p.getTotalElements());}
    public Results list(String shop,String actor,int page) {owned(shop,actor,false);return response(products.findByShopId(shop,page(page)));}
    public Results published(String shop,int page) {
        shops.findById(shop).filter(s->s.getStatus()==ShopStatus.ACTIVE).orElseThrow(ManagerProductService::missing);
        return response(products.findByShopIdAndStatus(shop,ProductStatus.ACTIVE,page(page)));
    }
    public List<CategoryOption> categories() {return categories.findByStatusOrderByNameAsc(CategoryStatus.ACTIVE).stream().map(c->new CategoryOption(c.getId(),c.getName())).toList();}
    @Transactional public Result save(String shopId,String id,String actor,Input input) {
        var shop=owned(shopId,actor,true);
        var product=id==null?new Product():products.findByIdAndShopId(id,shopId).orElseThrow(ManagerProductService::missing);
        var category=categories.findById(input.categoryId()).filter(c->c.getStatus()==CategoryStatus.ACTIVE).orElseThrow(()->new IllegalArgumentException("Danh mục không hợp lệ hoặc đã ngừng hoạt động."));
        if(id==null) {product.setId(UUID.randomUUID().toString());product.setShop(shop);product.setCreatedBy(actor);}
        product.setName(input.name().strip());product.setDescription(input.description().strip());product.setCategory(category);product.setPrice(input.price());product.setStock(input.stock());product.setStatus(input.status());product.setLastModifyBy(actor);
        return result(products.saveAndFlush(product));
    }
    @Transactional public void hide(String shopId,String id,String actor) {
        owned(shopId,actor,true);var p=products.findByIdAndShopId(id,shopId).orElseThrow(ManagerProductService::missing);
        p.setStatus(ProductStatus.INACTIVE);p.setLastModifyBy(actor);
    }
}
