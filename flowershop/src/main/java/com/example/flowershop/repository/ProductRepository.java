package com.example.flowershop.repository;

import java.util.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.example.flowershop.entity.Product;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.*;
import com.example.flowershop.entity.enums.ProductStatus;

/** Explicit Hibernate queries; Spring does not generate this repository. */
@Repository
public class ProductRepository extends HibernateRepository<Product> {
    public ProductRepository() { super(Product.class); }
    public long countForShop(String shopId) { return countWhere("e.shop.id = :shopId and e.deleted = false",Map.of("shopId",shopId)); }

    @Transactional
    public Optional<Product> findByIdForUpdate(String id) {
        return locked("e.id = :id", Map.of("id", id));
    }

    public Page<Product> findByShopId(String shopId, Pageable pageable) {
        return page("e.shop.id = :shopId and e.deleted = false", Map.of("shopId", shopId), pageable);
    }

    public Page<Product> findByShopIdAndStatus(String shopId, ProductStatus status, Pageable pageable) {
        return page("e.shop.id = :shopId and e.deleted = false and e.status = :status", Map.of("shopId", shopId, "status", status), pageable);
    }

    public Optional<Product> findByIdAndShopId(String id, String shopId) {
        return one("e.id = :id and e.shop.id = :shopId and e.deleted = false", Map.of("id", id, "shopId", shopId));
    }

    public List<Object[]> countVisibleByCategory() {
        return session().createSelectionQuery("select e.category.id, count(e) from Product e where e.status = com.example.flowershop.entity.enums.ProductStatus.ACTIVE and e.adminHidden = false and e.shop.status = com.example.flowershop.entity.enums.ShopStatus.ACTIVE group by e.category.id", Object[].class).getResultList();
    }
public boolean existsByCategoryId(String categoryId) { return countWhere("e.category.id = :id", Map.of("id",categoryId)) > 0; }
}
