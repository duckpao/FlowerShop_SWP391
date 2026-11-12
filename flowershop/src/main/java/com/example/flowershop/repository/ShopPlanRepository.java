package com.example.flowershop.repository;
import com.example.flowershop.entity.ShopPlan;
import org.springframework.stereotype.Repository;
@Repository public class ShopPlanRepository extends HibernateRepository<ShopPlan> {
    public ShopPlanRepository() { super(ShopPlan.class); }
    public java.util.Optional<ShopPlan> findLocked(String id) { return locked("e.id = :id",java.util.Map.of("id",id)); }
}
