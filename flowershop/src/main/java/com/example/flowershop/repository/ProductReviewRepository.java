package com.example.flowershop.repository;

import com.example.flowershop.entity.ProductReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

// Truy vấn Product_Reviews cho danh sách/đánh giá cá nhân và tính sao trung bình.
public interface ProductReviewRepository extends JpaRepository<ProductReview, String> {
    // mine/write/update/delete dùng cặp sản phẩm + người dùng để xác định review cá nhân.
    Optional<ProductReview> findByProductIdAndUserId(String productId, String userId);
    Page<ProductReview> findByProductId(String productId, Pageable pageable);

    // JPQL dùng tên entity/thuộc tính Java; Hibernate dịch sang SQL AVG/GROUP BY trên Product_Reviews.
    @Query("select r.product.id, avg(r.rating), count(r) from ProductReview r "
         + "where r.product.id in :ids group by r.product.id")
    List<Object[]> summarize(@Param("ids") Collection<String> ids);
}
