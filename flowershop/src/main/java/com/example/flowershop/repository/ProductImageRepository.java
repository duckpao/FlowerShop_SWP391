package com.example.flowershop.repository;

import com.example.flowershop.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductImageRepository extends JpaRepository<ProductImage, String> {
    List<ProductImage> findByProductIdOrderByDisplayOrderAscIdAsc(String productId);
    List<ProductImage> findByProductIdOrderByDisplayOrderAsc(String productId);
    List<ProductImage> findByProductIdInOrderByDisplayOrderAscIdAsc(Collection<String> productIds);
    long countByProductId(String productId);
    Optional<ProductImage> findByIdAndProductId(String id, String productId);
    void deleteByProductId(String productId);
}
