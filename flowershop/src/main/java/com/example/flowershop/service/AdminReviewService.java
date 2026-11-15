package com.example.flowershop.service;

import com.example.flowershop.entity.ProductReview;
import com.example.flowershop.repository.ProductReviewRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Admin kiểm duyệt đánh giá sản phẩm. Xóa là xóa hẳn dòng: điểm trung bình của sản phẩm
 * (tính từ bảng đánh giá) tự cập nhật, và khách có thể đánh giá lại đơn đó nếu muốn.
 */
@Service
@Transactional(readOnly = true)
public class AdminReviewService {
    private static final Logger log = LoggerFactory.getLogger(AdminReviewService.class);

    public record DeleteInput(@Size(max = 500) String reason) {}

    public record ReviewRow(String id, String productId, String productName, String shopId, String shopName,
                            String reviewerId, String reviewerName, int rating, String comment, String shopReply,
                            LocalDateTime createdDate) {}

    public record ReviewPage(List<ReviewRow> content, int page, int size, long totalElements, int totalPages) {}

    private final ProductReviewRepository reviews;
    private final NotificationService notifications;

    public AdminReviewService(ProductReviewRepository reviews, NotificationService notifications) {
        this.reviews = reviews;
        this.notifications = notifications;
    }

    private static String like(String raw) {
        return "%" + raw.strip().toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }

    public ReviewPage list(Integer rating, String productId, String shopId, String q, int page, int size) {
        if (page < 0 || page > 100000 || size < 1 || size > 100 || (q != null && q.length() > 100))
            throw new IllegalArgumentException("Trang phải từ 0 đến 100000, kích thước từ 1 đến 100, từ khóa tối đa 100 ký tự.");
        if (rating != null && (rating < 1 || rating > 5))
            throw new IllegalArgumentException("Số sao phải từ 1 đến 5.");
        String term = q == null || q.isBlank() ? null : like(q);
        Specification<ProductReview> spec = (root, cq, cb) -> {
            var product = root.join("product");
            var shop = product.join("shop");
            var user = root.join("user");
            var predicates = new ArrayList<Predicate>();
            if (rating != null) predicates.add(cb.equal(root.get("rating"), rating));
            if (productId != null && !productId.isBlank()) predicates.add(cb.equal(product.get("id"), productId.strip()));
            if (shopId != null && !shopId.isBlank()) predicates.add(cb.equal(shop.get("id"), shopId.strip()));
            if (term != null) predicates.add(cb.or(
                    cb.like(cb.lower(cb.coalesce(root.<String>get("comment"), "")), term, '!'),
                    cb.like(cb.lower(product.<String>get("name")), term, '!'),
                    cb.like(cb.lower(shop.<String>get("name")), term, '!'),
                    cb.like(cb.lower(cb.coalesce(user.<String>get("fullName"), "")), term, '!')));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        var result = reviews.findAll(spec, PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("createdDate"), Sort.Order.asc("id"))));
        return new ReviewPage(result.getContent().stream().map(AdminReviewService::row).toList(),
                page, size, result.getTotalElements(), result.getTotalPages());
    }

    private static ReviewRow row(ProductReview r) {
        var product = r.getProduct();
        String name = r.getUser().getFullName();
        return new ReviewRow(r.getId(), product.getId(), product.getName(), product.getShop().getId(),
                product.getShop().getName(), r.getUser().getId(),
                name == null || name.isBlank() ? "Khách hàng" : name,
                r.getRating() == null ? 0 : r.getRating(), r.getComment(), r.getShopReply(), r.getCreatedDate());
    }

    @Transactional
    public void delete(String reviewId, String adminId, String reason) {
        ProductReview review = reviews.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đánh giá."));
        String why = reason == null || reason.isBlank() ? null : reason.strip();
        var product = review.getProduct();
        var author = review.getUser();
        reviews.delete(review);
        reviews.flush();
        log.info("Admin {} xóa đánh giá {} của khách {} về sản phẩm {}. Lý do: {}", adminId, reviewId,
                author.getId(), product.getId(), why == null ? "(không nêu)" : why);
        notifications.notifyUser(author, "Đánh giá đã bị gỡ",
                "Đánh giá của bạn về sản phẩm \"" + product.getName() + "\" đã bị quản trị viên gỡ"
                        + (why == null ? "." : ". Lý do: " + why + "."),
                "/products/" + product.getId());
    }
}
