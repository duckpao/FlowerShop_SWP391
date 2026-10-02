package com.example.flowershop.service;

import com.example.flowershop.entity.Product;
import com.example.flowershop.entity.ProductReview;
import com.example.flowershop.entity.Shop;
import com.example.flowershop.entity.enums.ShopStatus;
import com.example.flowershop.repository.ProductRepository;
import com.example.flowershop.repository.ProductReviewRepository;
import com.example.flowershop.repository.ShopRepository;
import com.example.flowershop.repository.UserRepository;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

// Nghiệp vụ review của Product Details. ReviewInput/ReplyInput nhận JSON, ReviewResult/Results là JSON trả về.
// Luồng hiện tại không kiểm tra đơn đã mua; order_id của review có thể null.
@Service
@Transactional(readOnly = true)
public class ProductReviewService {

    public record ReviewInput(@NotNull @Min(1) @Max(5) Integer rating,
                              @Size(max = 2000) String comment) {}

    public record ReviewResult(String id, String productId, String reviewerName, int rating,
                               String comment, String shopReply, LocalDateTime createdAt) {}

    public record Results(List<ReviewResult> content, int page, long totalElements, int totalPages) {}

    public record ReplyInput(@NotNull @Size(max = 2000) String reply) {}

    private final ProductReviewRepository reviews;
    private final ProductRepository products;
    private final UserRepository users;
    private final ShopRepository shops;

    public ProductReviewService(ProductReviewRepository reviews, ProductRepository products,
                                UserRepository users, ShopRepository shops) {
        this.reviews = reviews;
        this.products = products;
        this.users = users;
        this.shops = shops;
    }

    private static ResponseStatusException missing() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy dữ liệu.");
    }

    /** Chỉ trả tên hiển thị; email không bao giờ rời khỏi service này. */
    private static ReviewResult result(ProductReview r) {
        String name = r.getUser().getFullName();
        return new ReviewResult(r.getId(), r.getProduct().getId(),
                name == null || name.isBlank() ? "Khách hàng" : name,
                r.getRating(), r.getComment(), r.getShopReply(), r.getCreatedDate());
    }

    // Dùng quy tắc công khai của assembler, không cho list/write trên sản phẩm đã ẩn.
    private Product visibleProduct(String productId) {
        return products.findById(productId).filter(ProductCardAssembler::visible)
                .orElseThrow(ProductReviewService::missing);
    }

    // Kiểm tra sản phẩm công khai -> findByProductId phân trang 10 mục -> map ReviewResult.
    public Results list(String productId, int page) {
        if (page < 0 || page > 100000) throw new IllegalArgumentException("Trang không hợp lệ.");
        // Endpoint này công khai: phải áp dụng đúng quy tắc hiển thị, nếu không đánh giá của
        // sản phẩm bị ẩn vẫn lộ ra kèm tên người đánh giá, và id không tồn tại thành kênh dò dữ liệu.
        visibleProduct(productId);
        Page<ProductReview> result = reviews.findByProductId(productId, PageRequest.of(page, 10,
                Sort.by(Sort.Order.desc("createdDate"), Sort.Order.asc("id"))));
        return new Results(result.getContent().stream().map(ProductReviewService::result).toList(),
                page, result.getTotalElements(), result.getTotalPages());
    }

    /** Đánh giá của chính người đang đăng nhập, để giao diện mở sẵn chế độ sửa. 404 nếu chưa có. */
    // findByProductIdAndUserId tìm đúng review của người gọi; không có thì ném 404.
    public ReviewResult mine(String productId, String actor) {
        return result(reviews.findByProductIdAndUserId(productId, actor)
                .orElseThrow(ProductReviewService::missing));
    }

    @Transactional
    // Kiểm tra visible và chưa review -> tạo ProductReview với user/product -> saveAndFlush; trùng trả 409.
    public ReviewResult write(String productId, String actor, ReviewInput input) {
        Product product = visibleProduct(productId);
        if (reviews.findByProductIdAndUserId(productId, actor).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bạn đã đánh giá sản phẩm này.");
        ProductReview review = ProductReview.builder().id(UUID.randomUUID().toString())
                .product(product).user(users.findById(actor).orElseThrow(ProductReviewService::missing))
                .rating(input.rating()).comment(input.comment() == null ? null : input.comment().strip())
                .createdBy(actor).build();
        return result(reviews.saveAndFlush(review));
    }

    @Transactional
    // Tìm theo productId + actor để sửa đúng review của mình; ghi rating/comment bằng saveAndFlush.
    public ReviewResult update(String productId, String actor, ReviewInput input) {
        ProductReview review = reviews.findByProductIdAndUserId(productId, actor)
                .orElseThrow(ProductReviewService::missing);
        review.setRating(input.rating());
        review.setComment(input.comment() == null ? null : input.comment().strip());
        review.setLastModifyBy(actor);
        return result(reviews.saveAndFlush(review));
    }

    @Transactional
    // Tìm review của mình rồi repository.delete -> xóa bản ghi Product_Reviews.
    public void delete(String productId, String actor) {
        reviews.delete(reviews.findByProductIdAndUserId(productId, actor)
                .orElseThrow(ProductReviewService::missing));
    }

    @Transactional
    // Khóa shop, kiểm tra owner + shop ACTIVE + review thuộc shop -> ghi shopReply trên chính review đó.
    public ReviewResult reply(String shopId, String reviewId, String actor, String reply) {
        Shop shop = shops.findForUpdate(shopId).orElseThrow(ProductReviewService::missing);
        if (!shop.getOwner().getId().equals(actor)) throw missing();
        if (shop.getStatus() != ShopStatus.ACTIVE)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Shop phải đang hoạt động.");
        ProductReview review = reviews.findById(reviewId).orElseThrow(ProductReviewService::missing);
        if (!review.getProduct().getShop().getId().equals(shopId)) throw missing();
        review.setShopReply(reply == null || reply.isBlank() ? null : reply.strip());
        review.setLastModifyBy(actor);
        return result(reviews.saveAndFlush(review));
    }
}
