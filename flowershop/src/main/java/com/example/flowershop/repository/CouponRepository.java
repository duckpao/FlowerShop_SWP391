package com.example.flowershop.repository;

import com.example.flowershop.entity.Coupon;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Sort;
import java.util.*;

@Repository
public class CouponRepository extends HibernateRepository<Coupon> {
    public CouponRepository() { super(Coupon.class); }
    public Optional<Coupon> findByCodeIgnoreCase(String code) {
        return one("lower(e.code) = lower(:code)", Map.of("code", code));
    }
    public Optional<Coupon> findByCodeForUpdate(String code) {
        return locked("lower(e.code) = lower(:code)", Map.of("code", code));
    }
    public Optional<Coupon> findLocked(String id) { return locked("e.id = :id", Map.of("id", id)); }
    public List<Coupon> managed(String shopId) {
        return list("e.deleted = false and " + (shopId == null ? "e.shop is null" : "e.shop.id = :shopId"),
                shopId == null ? Map.of() : Map.of("shopId", shopId), Sort.by(Sort.Order.desc("createdDate"), Sort.Order.asc("id")));
    }
    public List<Coupon> available(String shopId) {
        return list("e.deleted = false and (e.shop is null or e.shop.id = :shopId)", Map.of("shopId", shopId));
    }
}
