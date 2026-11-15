package com.example.flowershop.service;

import com.example.flowershop.entity.Product;
import com.example.flowershop.entity.ProductImage;
import com.example.flowershop.entity.enums.ProductStatus;
import com.example.flowershop.entity.enums.ProductType;
import com.example.flowershop.entity.enums.ShopStatus;
import com.example.flowershop.repository.ProductImageRepository;
import com.example.flowershop.repository.ProductReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/** Gom ảnh chính và điểm đánh giá theo lô để tránh N+1 khi dựng danh sách sản phẩm. */
@Service
@Transactional(readOnly = true)
public class ProductCardAssembler {

    /**
     * available gộp mọi lý do không hiển thị; adminHidden tách riêng để trang kiểm duyệt của Admin
     * phân biệt được "Admin ẩn" với "shop tự ẩn" hoặc "shop bị khoá".
     */
    public record ProductCard(String id, String name, ProductType type, BigDecimal price, Integer stock,
            ProductStatus status, String shopId, String shopName, String categoryId,
            String categoryName, String imageUrl, double rating, long reviewCount,
            boolean available, boolean adminHidden) {}

    private final ProductImageRepository images;
    private final ProductReviewRepository reviews;

    public ProductCardAssembler(ProductImageRepository images, ProductReviewRepository reviews) {
        this.images = images;
        this.reviews = reviews;
    }

    /** Quy tắc hiển thị công khai duy nhất, dùng chung toàn hệ thống. */
    public static boolean visible(Product p) {
        return p.getStatus() == ProductStatus.ACTIVE && !p.isAdminHidden()
                && p.getShop().getStatus() == ShopStatus.ACTIVE;
    }

    /** Ảnh chính của từng sản phẩm; ưu tiên cờ isPrimary, nếu không có thì ảnh đầu theo displayOrder. */
    public Map<String, String> primaryImages(Collection<String> productIds) {
        if (productIds.isEmpty()) return Map.of();
        Map<String, String> result = new HashMap<>();
        for (ProductImage image : images.findByProductIdInOrderByDisplayOrderAscIdAsc(productIds)) {
            String key = image.getProduct().getId();
            if (Boolean.TRUE.equals(image.getIsPrimary())) result.put(key, image.getImageUrl());
            else result.putIfAbsent(key, image.getImageUrl());
        }
        return result;
    }

    /** productId -> [điểm trung bình, số lượt]. Sản phẩm chưa có review không xuất hiện trong map. */
    public Map<String, double[]> ratings(Collection<String> productIds) {
        if (productIds.isEmpty()) return Map.of();
        Map<String, double[]> result = new HashMap<>();
        for (Object[] row : reviews.summarize(productIds)) {
            result.put((String) row[0], new double[]{
                    row[1] == null ? 0d : ((Number) row[1]).doubleValue(),
                    row[2] == null ? 0d : ((Number) row[2]).doubleValue()});
        }
        return result;
    }

    /**
     * Yêu cầu các Product đang được quản lý trong transaction đang mở: Product.shop và
     * Product.category là LAZY nên entity detached sẽ ném LazyInitializationException.
     */
    public List<ProductCard> cards(List<Product> products) {
        List<String> ids = products.stream().map(Product::getId).toList();
        Map<String, String> image = primaryImages(ids);
        Map<String, double[]> rating = ratings(ids);
        return products.stream().map(p -> {
            double[] summary = rating.getOrDefault(p.getId(), new double[]{0d, 0d});
            return new ProductCard(p.getId(), p.getName(), p.getType(), p.getPrice(), p.getStock(), p.getStatus(),
                    p.getShop().getId(), p.getShop().getName(), p.getCategory().getId(),
                    p.getCategory().getName(), image.get(p.getId()),
                    Math.round(summary[0] * 10) / 10.0, (long) summary[1], visible(p), p.isAdminHidden());
        }).toList();
    }
}
