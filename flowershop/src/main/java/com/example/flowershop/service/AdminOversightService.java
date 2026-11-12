package com.example.flowershop.service;

import com.example.flowershop.entity.Order;
import com.example.flowershop.entity.ProductReview;
import com.example.flowershop.entity.enums.OrderStatus;
import com.example.flowershop.repository.OrderRepository;
import com.example.flowershop.repository.ProductReviewRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** SRS 4.4: Admin xem đơn hàng toàn hệ thống và kiểm duyệt (xóa) đánh giá vi phạm. */
@Service
public class AdminOversightService {

    public record OrderRow(String id, String shopName, String customerName, String customerPhone,
                           BigDecimal totalAmount, String status, LocalDateTime createdDate, String orderType) {}
    public record OrderPage(List<OrderRow> content, int page, int totalPages, long totalElements) {}
    public record ReviewRow(String id, String productId, String productName, String shopName, String userName,
                            int rating, String comment, String shopReply, LocalDateTime createdDate) {}
    public record ReviewPage(List<ReviewRow> content, int page, int totalPages, long totalElements) {}

    private final OrderRepository orders;
    private final ProductReviewRepository reviews;

    public AdminOversightService(OrderRepository orders, ProductReviewRepository reviews) {
        this.orders = orders;
        this.reviews = reviews;
    }

    private static PageRequest page(int page) {
        return PageRequest.of(Math.max(page, 0), 20, Sort.by(Sort.Order.desc("createdDate"), Sort.Order.asc("id")));
    }

    @Transactional(readOnly = true)
    public OrderPage orders(String status, int page) {
        Page<Order> result;
        if (status == null || status.isBlank()) result = orders.findAll(page(page));
        else {
            try {
                result = orders.findByStatus(OrderStatus.valueOf(status.toUpperCase()), page(page));
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trạng thái không hợp lệ: " + status);
            }
        }
        var rows = result.map(o -> new OrderRow(o.getId(), o.getShop().getName(), o.getCustomer().getFullName(),
                o.getCustomer().getPhone(), o.getTotalAmount(), o.getStatus().name(), o.getCreatedDate(),
                o.getOrderType().name())).getContent();
        return new OrderPage(rows, result.getNumber(), result.getTotalPages(), result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public ReviewPage reviews(int page) {
        Page<ProductReview> result = reviews.findAll(page(page));
        var rows = result.map(r -> new ReviewRow(r.getId(), r.getProduct().getId(), r.getProduct().getName(),
                r.getProduct().getShop().getName(), r.getUser().getFullName(), r.getRating(), r.getComment(),
                r.getShopReply(), r.getCreatedDate())).getContent();
        return new ReviewPage(rows, result.getNumber(), result.getTotalPages(), result.getTotalElements());
    }

    @Transactional
    public void deleteReview(String id) {
        reviews.delete(reviews.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đánh giá.")));
    }
}
