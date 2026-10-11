package com.example.flowershop.dto.product;

import com.example.flowershop.dto.category.CategoryCardResponse;
import com.example.flowershop.dto.category.CategoryOptionResponse;
import com.example.flowershop.dto.category.CategoryResponse;
import com.example.flowershop.dto.review.ReviewResponse;
import com.example.flowershop.entity.Category;
import com.example.flowershop.entity.Product;
import com.example.flowershop.entity.ProductImage;
import com.example.flowershop.entity.ProductReview;
import com.example.flowershop.entity.ProductVideo;

import java.util.List;
import com.example.flowershop.service.ProductCardAssembler;

/**
 * Mapper chuyển đổi giữa JPA Entities và DTOs cho các chức năng Catalog, Sản phẩm, Danh mục, Đánh giá.
 */
public final class ProductMapper {

    private ProductMapper() {}

    public static ProductCardResponse fromCard(ProductCardAssembler.ProductCard card) {
        return new ProductCardResponse(
                card.id(),
                card.name(),
                card.price(),
                card.stock(),
                card.status(),
                card.shopId(),
                card.shopName(),
                card.categoryId(),
                card.categoryName(),
                card.imageUrl(),
                card.rating(),
                card.reviewCount(),
                card.available(),
                card.adminHidden()
        );
    }

    public static ProductCardResponse toCardResponse(Product p, String imageUrl, double rating, long reviewCount, boolean available) {
        return new ProductCardResponse(
                p.getId(),
                p.getName(),
                p.getPrice(),
                p.getStock(),
                p.getStatus(),
                p.getShop() != null ? p.getShop().getId() : null,
                p.getShop() != null ? p.getShop().getName() : null,
                p.getCategory() != null ? p.getCategory().getId() : null,
                p.getCategory() != null ? p.getCategory().getName() : null,
                imageUrl,
                rating,
                reviewCount,
                available,
                p.isAdminHidden()
        );
    }

    public static ProductDetailResponse toDetailResponse(Product p, List<String> images, Double rating, int reviewCount) {
        return new ProductDetailResponse(
                p.getId(),
                p.getName(),
                p.getPrice(),
                p.getStock(),
                p.getDescription(),
                p.getShop() != null ? p.getShop().getId() : null,
                p.getShop() != null ? p.getShop().getName() : null,
                p.getCategory() != null ? p.getCategory().getId() : null,
                p.getCategory() != null ? p.getCategory().getName() : null,
                images != null ? images : List.of(),
                rating,
                reviewCount
        );
    }

    public static ProductResponse toResponse(Product p, List<String> images) {
        return new ProductResponse(
                p.getId(),
                p.getShop() != null ? p.getShop().getId() : null,
                p.getCategory() != null ? p.getCategory().getId() : null,
                p.getName(),
                p.getPrice(),
                p.getStock(),
                p.getDescription(),
                p.getStatus(),
                p.isAdminHidden(),
                images != null ? images : List.of()
        );
    }

    public static ProductImageResponse toImageResponse(ProductImage image) {
        return new ProductImageResponse(
                image.getId(),
                image.getImageUrl(),
                Boolean.TRUE.equals(image.getIsPrimary()),
                image.getDisplayOrder() != null ? image.getDisplayOrder() : 0
        );
    }

    public static ProductVideoResponse toVideoResponse(ProductVideo video) {
        return new ProductVideoResponse(
                video.getId(),
                video.getVideoUrl(),
                video.getTitle(),
                video.getDescription(),
                video.getDisplayOrder() != null ? video.getDisplayOrder() : 0
        );
    }

    public static CategoryCardResponse toCategoryCardResponse(Category c, long productCount) {
        return new CategoryCardResponse(
                c.getId(),
                c.getName(),
                c.getDescription(),
                productCount
        );
    }

    public static CategoryOptionResponse toCategoryOptionResponse(Category c) {
        return new CategoryOptionResponse(
                c.getId(),
                c.getName()
        );
    }

    public static CategoryResponse toCategoryResponse(Category c, long productCount) {
        return new CategoryResponse(
                c.getId(),
                c.getName(),
                c.getDescription(),
                c.getStatus(),
                productCount
        );
    }

    public static ReviewResponse toReviewResponse(ProductReview r) {
        String name = r.getUser() != null ? r.getUser().getFullName() : null;
        return new ReviewResponse(
                r.getId(),
                r.getProduct() != null ? r.getProduct().getId() : null,
                name == null || name.isBlank() ? "Khách hàng" : name,
                r.getRating() != null ? r.getRating() : 0,
                r.getComment(),
                r.getShopReply(),
                r.getCreatedDate()
        );
    }
}
