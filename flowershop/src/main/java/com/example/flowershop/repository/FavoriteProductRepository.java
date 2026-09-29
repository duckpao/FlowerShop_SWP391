package com.example.flowershop.repository;

import com.example.flowershop.entity.FavoriteProduct;
import com.example.flowershop.entity.FavoriteProductId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FavoriteProductRepository extends JpaRepository<FavoriteProduct, FavoriteProductId> {
    @Query("select f from FavoriteProduct f where f.id.userId = :userId")
    Page<FavoriteProduct> findOwned(@Param("userId") String userId, Pageable pageable);
}
