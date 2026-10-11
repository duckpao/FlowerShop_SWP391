package com.example.flowershop.service;

import com.example.flowershop.entity.FavoriteShop;
import com.example.flowershop.entity.FavoriteShopId;
import com.example.flowershop.entity.Shop;
import com.example.flowershop.entity.enums.ShopStatus;
import com.example.flowershop.repository.FavoriteShopRepository;
import com.example.flowershop.repository.ShopRepository;
import com.example.flowershop.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class FavoriteShopService {

    public record ShopFavoriteResponse(
            String shopId,
            String name,
            String description,
            String logoUrl,
            boolean active,
            LocalDateTime followedAt) {
    }

    public record FavoriteStatus(boolean favorite) {
    }

    private final FavoriteShopRepository favorites;
    private final ShopRepository shops;
    private final UserRepository users;

    public FavoriteShopService(
            FavoriteShopRepository favorites,
            ShopRepository shops,
            UserRepository users) {
        this.favorites = favorites;
        this.shops = shops;
        this.users = users;
    }

    /**
     * Follow a shop.
     */
    @Transactional
    public void add(String userId, String shopId) {

        Shop shop = shops.findById(shopId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy cửa hàng."));

        if (shop.getStatus() != ShopStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cửa hàng hiện không hoạt động.");
        }

        FavoriteShopId id = new FavoriteShopId(userId, shopId);

        // Already followed -> do nothing
        if (favorites.existsById(id)) {
            return;
        }

        FavoriteShop favorite = FavoriteShop.builder()
                .id(id)
                .user(
                        users.findById(userId)
                                .orElseThrow(() -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Không tìm thấy người dùng.")))
                .shop(shop)
                .createdBy(userId)
                .lastModifyBy(userId)
                .build();

        favorites.saveAndFlush(favorite);
    }

    /**
     * Unfollow a shop.
     */
    @Transactional
    public void remove(String userId, String shopId) {

        FavoriteShopId id = new FavoriteShopId(userId, shopId);

        /*
         * Use findById first so that unfollowing an already
         * unfollowed shop does not cause an exception.
         */
        favorites.findById(id)
                .ifPresent(favorites::delete);
    }

    /**
     * Check whether current user follows a shop.
     */
    public FavoriteStatus status(
            String userId,
            String shopId) {

        FavoriteShopId id = new FavoriteShopId(userId, shopId);

        return new FavoriteStatus(
                favorites.existsById(id));
    }

    /**
     * Get all shops followed by current user.
     */
    public List<ShopFavoriteResponse> mine(String userId) {

        return favorites
                .findByIdUserIdOrderByCreatedDateDesc(userId)
                .stream()
                .map(FavoriteShopService::toResponse)
                .toList();
    }

    private static ShopFavoriteResponse toResponse(
            FavoriteShop favorite) {

        Shop shop = favorite.getShop();

        return new ShopFavoriteResponse(
                shop.getId(),
                shop.getName(),
                shop.getDescription(),
                shop.getLogoUrl(),
                shop.getStatus() == ShopStatus.ACTIVE,
                favorite.getCreatedDate());
    }
}