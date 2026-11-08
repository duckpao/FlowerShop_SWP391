package com.example.flowershop.repository;
import com.example.flowershop.entity.ShopSubscription;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.*;
@Repository public class ShopSubscriptionRepository extends HibernateRepository<ShopSubscription> {
    public ShopSubscriptionRepository() { super(ShopSubscription.class); }
    public Optional<ShopSubscription> forShop(String shopId) { return one("e.shop.id = :shopId",Map.of("shopId",shopId)); }
    public List<String> dueIds(LocalDateTime now) {
        return session().createSelectionQuery("select e.id from ShopSubscription e where e.endsAt > :now and e.endsAt <= :until",String.class)
            .setParameter("now",now).setParameter("until",now.toLocalDate().plusDays(3).atStartOfDay()).getResultList();
    }
}
