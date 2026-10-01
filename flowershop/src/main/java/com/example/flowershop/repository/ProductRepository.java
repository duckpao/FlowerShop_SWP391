package com.example.flowershop.repository;
import com.example.flowershop.entity.Product;
import com.example.flowershop.entity.enums.ProductStatus;
import com.example.flowershop.entity.enums.ShopStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import java.util.Optional;
public interface ProductRepository extends JpaRepository<Product,String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") String id);
    Page<Product> findByShopId(String shopId,Pageable pageable);
    Page<Product> findByShopIdAndStatus(String shopId,ProductStatus status,Pageable pageable);
    Optional<Product> findByIdAndShopId(String id,String shopId);
    @Query("SELECT p FROM Product p WHERE p.status = :status AND p.shop.status = :shopStatus " +
           "AND (:categoryId IS NULL OR p.category.id = :categoryId) " +
           "AND (:q IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Product> searchCatalog(@Param("status") ProductStatus status,@Param("shopStatus") ShopStatus shopStatus,
                                 @Param("categoryId") String categoryId,@Param("q") String q,Pageable pageable);
}
