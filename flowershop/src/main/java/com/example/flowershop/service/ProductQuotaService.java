package com.example.flowershop.service;

import com.example.flowershop.entity.Product;
import com.example.flowershop.entity.ShopSubscription;
import com.example.flowershop.repository.ProductRepository;
import com.example.flowershop.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.*;

/** Live entitlement check: applies to imported data, expiry and admin changes without a job delay. */
@Service @Transactional(readOnly=true)
public class ProductQuotaService {
    private final ProductRepository products;
    private final ShopPlanClock clock;
    public ProductQuotaService(ProductRepository products,ShopPlanClock clock) { this.products=products;this.clock=clock; }
    public Specification<Product> allowedSpecification() {
        LocalDateTime now=clock.now();
        return (root,query,cb)->{
            var rank=query.subquery(Long.class);var earlier=rank.from(Product.class);
            rank.select(cb.count(earlier)).where(cb.equal(earlier.get("shop").get("id"),root.get("shop").get("id")),
                    cb.isFalse(earlier.get("deleted")),cb.or(
                            cb.lessThan(earlier.<LocalDateTime>get("createdDate"),root.get("createdDate")),
                            cb.and(cb.equal(earlier.get("createdDate"),root.get("createdDate")),cb.lessThanOrEqualTo(earlier.<String>get("id"),root.get("id")))));
            var limit=query.subquery(Integer.class);var s=limit.from(ShopSubscription.class);
            limit.select(s.get("plan").get("productLimit")).where(cb.equal(s.get("shop").get("id"),root.get("shop").get("id")),cb.greaterThan(s.get("endsAt"),now));
            var unlimited=query.subquery(String.class);var u=unlimited.from(ShopSubscription.class);
            unlimited.select(u.get("id")).where(cb.equal(u.get("shop").get("id"),root.get("shop").get("id")),cb.greaterThan(u.get("endsAt"),now),cb.isNull(u.get("plan").get("productLimit")));
            return cb.and(cb.isFalse(root.get("deleted")),cb.or(cb.exists(unlimited),cb.le(rank,cb.coalesce(limit,5))));
        };
    }
    public Set<String> allowedIds(Collection<Product> candidates) {
        if(candidates.isEmpty()) return Set.of();
        var ids=candidates.stream().map(Product::getId).toList();
        Specification<Product> selected=(root,query,cb)->root.get("id").in(ids);
        return new HashSet<>(products.findAll(allowedSpecification().and(selected),Pageable.unpaged()).getContent().stream().map(Product::getId).toList());
    }
    public boolean allowed(Product product) { return allowedIds(List.of(product)).contains(product.getId()); }
    public void requireAllowed(Product product) {
        if(!allowed(product)) throw ApiException.badRequest("Sản phẩm đang bị khóa do vượt giới hạn gói của shop. Shop cần nâng cấp gói hoặc xóa bớt sản phẩm.");
    }
}
