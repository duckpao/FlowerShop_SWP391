package com.example.flowershop.repository;
import com.example.flowershop.entity.Product;
import com.example.flowershop.entity.enums.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;
import java.util.Optional;
public interface ProductRepository extends JpaRepository<Product,String> {
    Page<Product> findByShopId(String shopId,Pageable pageable);
    Page<Product> findByShopIdAndStatus(String shopId,ProductStatus status,Pageable pageable);
    Optional<Product> findByIdAndShopId(String id,String shopId);
}
