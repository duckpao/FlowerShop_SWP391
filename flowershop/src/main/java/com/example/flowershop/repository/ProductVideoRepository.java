package com.example.flowershop.repository;

import com.example.flowershop.entity.ProductVideo;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Sort;
import java.util.*;

/** Explicit Hibernate queries; Spring does not generate this repository. */
@Repository
public class ProductVideoRepository extends HibernateRepository<ProductVideo> {
    public ProductVideoRepository() { super(ProductVideo.class); }

    public List<ProductVideo> findByProductIdOrderByDisplayOrderAsc(String productId) {
        return list("e.product.id = :productId", Map.of("productId", productId), Sort.by("displayOrder"));
    }

    public long countByProductId(String productId) {
        return countWhere("e.product.id = :productId", Map.of("productId", productId));
    }

    public Optional<ProductVideo> findByIdAndProductId(String id, String productId) {
        return one("e.id = :id and e.product.id = :productId", Map.of("id", id, "productId", productId));
    }
}
