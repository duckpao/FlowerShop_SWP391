package com.example.flowershop.repository;

import com.example.flowershop.entity.Blog;
import com.example.flowershop.entity.enums.BlogStatus;
import com.example.flowershop.entity.enums.ShopStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BlogRepository extends JpaRepository<Blog, String> {
    Page<Blog> findByShopId(String shopId, Pageable pageable);

    Optional<Blog> findByIdAndShopId(String id, String shopId);

    Optional<Blog> findByIdAndStatusAndShop_Status(String id, BlogStatus status, ShopStatus shopStatus);

    @Query("select b from Blog b where b.status = :status and b.shop.status = :shopStatus "
            + "and (:shopId is null or b.shop.id = :shopId) "
            + "and (:q = '' or lower(b.title) like lower(concat('%', :q, '%')) "
            + "or lower(b.content) like lower(concat('%', :q, '%')))")
    Page<Blog> searchPublic(@Param("status") BlogStatus status,
            @Param("shopStatus") ShopStatus shopStatus,
            @Param("shopId") String shopId,
            @Param("q") String q,
            Pageable pageable);
}
