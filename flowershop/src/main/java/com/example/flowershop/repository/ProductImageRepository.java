package com.example.flowershop.repository;

import com.example.flowershop.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ProductImageRepository extends JpaRepository<ProductImage, String> {
    List<ProductImage> findByProductIdOrderByDisplayOrderAscIdAsc(String productId);
    List<ProductImage> findByProductIdInOrderByDisplayOrderAscIdAsc(Collection<String> productIds);
    void deleteByProductId(String productId);
}
