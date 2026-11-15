package com.example.flowershop.repository;
import com.example.flowershop.entity.FeaturedProduct;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Sort;
import java.time.LocalDateTime;
import java.util.*;
@Repository public class FeaturedProductRepository extends HibernateRepository<FeaturedProduct> {
    public FeaturedProductRepository() { super(FeaturedProduct.class); }
    public List<FeaturedProduct> forShop(String shopId) { return list("e.shop.id = :shopId",Map.of("shopId",shopId),Sort.by("featuredAt")); }
    public long countForShop(String shopId) { return countWhere("e.shop.id = :shopId",Map.of("shopId",shopId)); }
    public List<FeaturedProduct> visible(LocalDateTime now) {
        return selection("e.product.status = com.example.flowershop.entity.enums.ProductStatus.ACTIVE and e.product.adminHidden = false and e.product.stock > 0 and e.shop.status = com.example.flowershop.entity.enums.ShopStatus.ACTIVE and e.shop.owner.status = com.example.flowershop.entity.enums.UserStatus.ACTIVE and e.product.category.status = com.example.flowershop.entity.enums.CategoryStatus.ACTIVE and exists (select s.id from ShopSubscription s where s.shop.id = e.shop.id and s.endsAt > :now)",
            Map.of("now",now),Sort.by(Sort.Order.desc("featuredAt"),Sort.Order.asc("id"))).getResultList();
    }
}
