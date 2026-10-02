package com.example.flowershop.repository;

import com.example.flowershop.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

// Truy vấn Product_Images: findBy... được Spring Data suy ra từ tên method; OrderBy quy định thứ tự SQL.
public interface ProductImageRepository extends JpaRepository<ProductImage, String> {
    // Gallery chi tiết và form Shop: lấy ảnh của một sản phẩm theo displayOrder, rồi id.
    List<ProductImage> findByProductIdOrderByDisplayOrderAscIdAsc(String productId);
    List<ProductImage> findByProductIdOrderByDisplayOrderAsc(String productId);
    // Assembler đọc ảnh của nhiều sản phẩm cùng lúc với IN, giảm số truy vấn lấy ảnh.
    List<ProductImage> findByProductIdInOrderByDisplayOrderAscIdAsc(Collection<String> productIds);
    long countByProductId(String productId);
    Optional<ProductImage> findByIdAndProductId(String id, String productId);
    // ManagerProductService.save gọi khi thay toàn bộ danh sách URL ảnh của sản phẩm.
    void deleteByProductId(String productId);
}
