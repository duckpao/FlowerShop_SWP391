package com.example.flowershop.repository;
import com.example.flowershop.entity.ShopProductPosting;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.*;
@Repository public class ShopProductPostingRepository extends HibernateRepository<ShopProductPosting> {
    public ShopProductPostingRepository() { super(ShopProductPosting.class); }
    public long inMonth(String shopId,LocalDateTime start,LocalDateTime end) {
        return countWhere("e.shopId = :shopId and e.postedAt >= :start and e.postedAt < :end",Map.of("shopId",shopId,"start",start,"end",end));
    }
}
