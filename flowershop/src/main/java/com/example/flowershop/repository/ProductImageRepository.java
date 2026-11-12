package com.example.flowershop.repository;

import com.example.flowershop.entity.ProductImage;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Sort;
import java.util.*;

/** Explicit Hibernate queries; Spring does not generate this repository. */
@Repository
public class ProductImageRepository extends HibernateRepository<ProductImage> {
    public ProductImageRepository() { super(ProductImage.class); }

    public List<ProductImage> findByProductIdOrderByDisplayOrderAsc(String productId) {
        return list("e.product.id = :productId", Map.of("productId", productId), Sort.by("displayOrder"));
    }

    public long countByProductId(String productId) {
        return countWhere("e.product.id = :productId", Map.of("productId", productId));
    }

    public Optional<ProductImage> findByIdAndProductId(String id, String productId) {
        return one("e.id = :id and e.product.id = :productId", Map.of("id", id, "productId", productId));
    }

    public List<ProductImage> findByProductIdOrderByDisplayOrderAscIdAsc(String productId) {
        return list("e.product.id = :productId", Map.of("productId", productId), Sort.by("displayOrder", "id"));
    }

    public List<ProductImage> findByProductIdInOrderByDisplayOrderAscIdAsc(Collection<String> productIds) {
        if (productIds.isEmpty()) return List.of(); return list("e.product.id in :productIds", Map.of("productIds", productIds), Sort.by("displayOrder", "id"));
    }

    @Transactional
    public void deleteByProductId(String productId) {
        for (ProductImage image : findByProductIdOrderByDisplayOrderAsc(productId)) delete(image);
    }
}
