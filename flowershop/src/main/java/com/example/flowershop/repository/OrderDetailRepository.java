package com.example.flowershop.repository;

import com.example.flowershop.entity.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.time.LocalDateTime;

public interface OrderDetailRepository extends JpaRepository<OrderDetail, String> {
    List<OrderDetail> findByOrderId(String orderId);

    /** Số lượng sản phẩm mà chính khách này đang giữ trong các đơn PENDING (kho đã bị trừ nhưng giỏ vẫn còn). */
    @Query("SELECT COALESCE(SUM(d.quantity), 0) FROM OrderDetail d WHERE d.order.customer.id = :userId "
            + "AND d.order.status = com.example.flowershop.entity.enums.OrderStatus.PENDING AND d.product.id = :productId")
    int sumPendingQuantity(@Param("userId") String userId, @Param("productId") String productId);

    /** Các đơn PENDING của khách có chứa một trong những sản phẩm này. */
    @Query("SELECT DISTINCT d.order.id FROM OrderDetail d WHERE d.order.customer.id = :userId "
            + "AND d.order.status = com.example.flowershop.entity.enums.OrderStatus.PENDING AND d.product.id IN :productIds")
    List<String> findPendingOrderIds(@Param("userId") String userId, @Param("productIds") Collection<String> productIds);

    List<OrderDetail> findByOrderIdIn(Collection<String> orderIds);

    @Query("SELECT d.product.id, d.product.name, "
            + "SUM(CASE WHEN d.order.status = com.example.flowershop.entity.enums.OrderStatus.COMPLETED THEN d.quantity ELSE 0 END), "
            + "SUM(CASE WHEN d.order.status = com.example.flowershop.entity.enums.OrderStatus.REFUNDED THEN d.quantity ELSE 0 END), "
            + "COUNT(DISTINCT CASE WHEN d.order.status = com.example.flowershop.entity.enums.OrderStatus.COMPLETED THEN d.order.id ELSE NULL END), "
            + "COUNT(DISTINCT CASE WHEN d.order.status = com.example.flowershop.entity.enums.OrderStatus.REFUNDED THEN d.order.id ELSE NULL END) "
            + "FROM OrderDetail d WHERE d.order.shop.id = :shopId AND d.product IS NOT NULL "
            + "AND d.order.createdDate >= :start AND d.order.createdDate < :end "
            + "AND d.order.status IN (com.example.flowershop.entity.enums.OrderStatus.COMPLETED, "
            + "com.example.flowershop.entity.enums.OrderStatus.REFUNDED) "
            + "GROUP BY d.product.id, d.product.name ORDER BY d.product.name")
    List<Object[]> productPerformanceForReport(@Param("shopId") String shopId,
            @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
