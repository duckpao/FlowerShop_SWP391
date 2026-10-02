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
// JPA làm cầu nối tới Products: JpaRepository có sẵn findById/saveAndFlush/delete; JpaSpecificationExecutor có findAll(spec, pageable).
// Spring tạo implementation lúc chạy; vì vậy không có file ProductRepositoryImpl viết tay trong dự án.
public interface ProductRepository extends JpaRepository<Product,String>, JpaSpecificationExecutor<Product> {
    // Shop xem tất cả sản phẩm thuộc shopId; Pageable được chuyển thành truy vấn phân trang.
    Page<Product> findByShopId(String shopId,Pageable pageable);
    Page<Product> findByShopIdAndStatus(String shopId,ProductStatus status,Pageable pageable);
    // Tìm sản phẩm đồng thời theo id và shopId, tránh sửa/ẩn sản phẩm thuộc shop khác.
    Optional<Product> findByIdAndShopId(String id,String shopId);

    /** Đếm sản phẩm đang hiển thị công khai theo từng danh mục. */
    @Query("select p.category.id, count(p) from Product p "
         + "where p.status = com.example.flowershop.entity.enums.ProductStatus.ACTIVE "
         + "and p.adminHidden = false "
         + "and p.shop.status = com.example.flowershop.entity.enums.ShopStatus.ACTIVE "
         + "group by p.category.id")
    List<Object[]> countVisibleByCategory();
}
