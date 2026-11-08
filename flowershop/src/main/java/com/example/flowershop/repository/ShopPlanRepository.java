package com.example.flowershop.repository;
import com.example.flowershop.entity.ShopPlan;
import org.springframework.stereotype.Repository;
@Repository public class ShopPlanRepository extends HibernateRepository<ShopPlan> {
    public ShopPlanRepository() { super(ShopPlan.class); }
}
