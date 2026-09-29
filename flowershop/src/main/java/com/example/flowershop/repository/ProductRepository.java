package com.example.flowershop.repository;
import com.example.flowershop.entity.Product;
import com.example.flowershop.entity.enums.ProductStatus;
import com.example.flowershop.entity.enums.ShopStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.*;
import java.util.List;
import java.util.Optional;
public interface ProductRepository extends JpaRepository<Product,String>, JpaSpecificationExecutor<Product> {
    Page<Product> findByShopId(String shopId,Pageable pageable);
    Page<Product> findByShopIdAndStatus(String shopId,ProductStatus status,Pageable pageable);
    Optional<Product> findByIdAndShopId(String id,String shopId);

    /** Đếm sản phẩm đang hiển thị công khai theo từng danh mục. */
    @Query("select p.category.id, count(p) from Product p "
         + "where p.status = com.example.flowershop.entity.enums.ProductStatus.ACTIVE "
         + "and p.adminHidden = false "
         + "and p.shop.status = com.example.flowershop.entity.enums.ShopStatus.ACTIVE "
         + "group by p.category.id")
    List<Object[]> countVisibleByCategory();
}
