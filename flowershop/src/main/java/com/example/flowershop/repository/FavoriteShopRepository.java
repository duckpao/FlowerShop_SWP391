package com.example.flowershop.repository;

import com.example.flowershop.entity.FavoriteShop;
import com.example.flowershop.entity.FavoriteShopId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FavoriteShopRepository
        extends JpaRepository<FavoriteShop, FavoriteShopId> {

    List<FavoriteShop> findByIdUserIdOrderByCreatedDateDesc(String userId);
}