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

public interface ProductReviewRepository extends JpaRepository<ProductReview, String> {
    Optional<ProductReview> findByProductIdAndUserId(String productId, String userId);
    Page<ProductReview> findByProductId(String productId, Pageable pageable);

    @Query("select r.product.id, avg(r.rating), count(r) from ProductReview r "
         + "where r.product.id in :ids group by r.product.id")
    List<Object[]> summarize(@Param("ids") Collection<String> ids);
}
