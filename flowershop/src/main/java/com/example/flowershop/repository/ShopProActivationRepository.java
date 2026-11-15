package com.example.flowershop.repository;
import com.example.flowershop.entity.ShopProActivation;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Sort;
import java.util.*;
@Repository public class ShopProActivationRepository extends HibernateRepository<ShopProActivation> {
    public ShopProActivationRepository(){super(ShopProActivation.class);}
    public List<ShopProActivation> history(String shopId) {
        return selection("e.shop.id = :shopId",Map.of("shopId",shopId),Sort.by(Sort.Order.desc("createdAt"),Sort.Order.asc("id"))).setMaxResults(50).getResultList();
    }
}
