package com.example.flowershop.service;

import com.example.flowershop.entity.Product;
import com.example.flowershop.entity.ProductImage;
import com.example.flowershop.entity.ProductVideo;
import com.example.flowershop.entity.enums.CategoryStatus;
import com.example.flowershop.entity.enums.ProductStatus;
import com.example.flowershop.entity.enums.ProductType;
import com.example.flowershop.entity.enums.ShopStatus;
import com.example.flowershop.repository.CategoryRepository;
import com.example.flowershop.repository.ProductImageRepository;
import com.example.flowershop.repository.ProductRepository;
import com.example.flowershop.repository.ProductVideoRepository;
import com.example.flowershop.repository.ShopRepository;
import com.example.flowershop.service.ProductCardAssembler.ProductCard;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class CatalogService {

    public record Results(List<ProductCard> content, int page, int size,
                          long totalElements, int totalPages) {}

    public record ProductDetail(String id, String name, ProductType type, String description, BigDecimal price,
            Integer stock, String shopId, String shopName, String categoryId, String categoryName,
            List<String> images, List<VideoInfo> videos, double rating, long reviewCount) {}

    public record VideoInfo(String videoUrl, String title, String description) {}

    public record CategoryCard(String id, String name, String description, long productCount) {}

    private final ProductRepository products;
    private final ProductCardAssembler assembler;
    private final CategoryRepository categories;
    private final ProductImageRepository images;
    private final ProductVideoRepository videos;
    private final ShopRepository shops;

    public CatalogService(ProductRepository products, ProductCardAssembler assembler,
                          CategoryRepository categories, ProductImageRepository images,
                          ProductVideoRepository videos, ShopRepository shops) {
        this.products = products;
        this.assembler = assembler;
        this.categories = categories;
        this.images = images;
        this.videos = videos;
        this.shops = shops;
    }

    private static ResponseStatusException missing() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm.");
    }

    public Results browse(String q, String categoryId, String shopId, String sort, int page, int size) {
        return browse(q, categoryId, shopId, null, null, null, sort, page, size);
    }

    public Results browse(String q, String categoryId, String shopId, BigDecimal minPrice,
                          BigDecimal maxPrice, String sort, int page, int size) {
        return browse(q, categoryId, shopId, null, minPrice, maxPrice, sort, page, size);
    }

    public Results browse(String q, String categoryId, String shopId, ProductType type,
                          BigDecimal minPrice, BigDecimal maxPrice, String sort, int page, int size) {
        if (page < 0 || page > 100000 || size < 1 || size > 100)
            throw new IllegalArgumentException("Trang từ 0 đến 100000, kích thước từ 1 đến 100.");
        if ((minPrice != null && minPrice.signum() < 0) || (maxPrice != null && maxPrice.signum() < 0)
                || (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0))
            throw new IllegalArgumentException("Khoảng giá không hợp lệ.");
        String raw = q == null ? "" : q.strip();
        if (raw.length() > 100) throw new IllegalArgumentException("Từ khóa tối đa 100 ký tự.");
        String term = raw.toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_");

        Sort order = switch (sort == null || sort.isBlank() ? "newest" : sort) {
            case "newest" -> Sort.by(Sort.Order.desc("createdDate"), Sort.Order.asc("id"));
            case "price_asc" -> Sort.by(Sort.Order.asc("price"), Sort.Order.asc("id"));
            case "price_desc" -> Sort.by(Sort.Order.desc("price"), Sort.Order.asc("id"));
            default -> throw new IllegalArgumentException("Cách sắp xếp không hợp lệ.");
        };

        Specification<Product> spec = (root, query, cb) -> {
            var shop = root.join("shop");
            var predicates = new ArrayList<Predicate>();
            predicates.add(cb.equal(root.get("status"), ProductStatus.ACTIVE));
            predicates.add(cb.isFalse(root.get("adminHidden")));
            predicates.add(cb.equal(shop.get("status"), ShopStatus.ACTIVE));
            if (categoryId != null && !categoryId.isBlank())
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            if (shopId != null && !shopId.isBlank())
                predicates.add(cb.equal(shop.get("id"), shopId));
            if (type != null) predicates.add(cb.equal(root.get("type"), type));
            if (minPrice != null) predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            if (maxPrice != null) predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            if (!term.isEmpty())
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + term + "%", '!'));
            return cb.and(predicates.toArray(Predicate[]::new));
        };

        Page<Product> result = products.findAll(spec, PageRequest.of(page, size, order));
        return new Results(assembler.cards(result.getContent()), page, size,
                result.getTotalElements(), result.getTotalPages());
    }

    public ProductDetail detail(String productId) {
        Product p = products.findById(productId).filter(ProductCardAssembler::visible)
                .orElseThrow(CatalogService::missing);
        // Ảnh chính đứng đầu gallery, phần còn lại giữ nguyên thứ tự displayOrder.
        List<String> urls = images.findByProductIdOrderByDisplayOrderAscIdAsc(productId).stream()
                .sorted(Comparator.comparingInt(i -> Boolean.TRUE.equals(i.getIsPrimary()) ? 0 : 1))
                .map(ProductImage::getImageUrl).toList();
        List<VideoInfo> videoInfos = videos.findByProductIdOrderByDisplayOrderAsc(productId).stream()
                .map(video -> new VideoInfo(video.getVideoUrl(), video.getTitle(), video.getDescription()))
                .toList();
        double[] summary = assembler.ratings(List.of(productId)).getOrDefault(productId, new double[]{0d, 0d});
        return new ProductDetail(p.getId(), p.getName(), p.getType(), p.getDescription(), p.getPrice(), p.getStock(),
                p.getShop().getId(), p.getShop().getName(), p.getCategory().getId(),
                p.getCategory().getName(), urls, videoInfos, Math.round(summary[0] * 10) / 10.0, (long) summary[1]);
    }

    public List<CategoryCard> categories() {
        Map<String, Long> counts = new HashMap<>();
        for (Object[] row : products.countVisibleByCategory())
            counts.put((String) row[0], ((Number) row[1]).longValue());
        return categories.findByStatusOrderByNameAsc(CategoryStatus.ACTIVE).stream()
                .map(c -> new CategoryCard(c.getId(), c.getName(), c.getDescription(),
                        counts.getOrDefault(c.getId(), 0L))).toList();
    }

    /** Sản phẩm đang bán của một shop; chuyển từ ManagerProductService sang đây. */
    public Results published(String shopId, int page) {
        shops.findById(shopId).filter(s -> s.getStatus() == ShopStatus.ACTIVE)
                .orElseThrow(CatalogService::missing);
        return browse("", null, shopId, "newest", page, 20);
    }
}
