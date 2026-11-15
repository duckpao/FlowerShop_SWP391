package com.example.flowershop.repository;

import com.example.flowershop.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface PaymentRepository extends JpaRepository<Payment, String> {
    List<Payment> findByOrderId(String orderId);

    Optional<Payment> findTopByOrderIdOrderByCreatedDateDesc(String orderId);

    Optional<Payment> findByInvoiceNumber(String invoiceNumber);

    List<Payment> findByGroupInvoice(String groupInvoice);

    /** Cập nhật thẳng trong DB để không ghi đè SUCCESS mà IPN vừa commit. */
    @Modifying
    @Query("UPDATE Payment p SET p.status = com.example.flowershop.entity.enums.PaymentStatus.FAILED, p.lastModifyBy = :actor "
            + "WHERE p.order.id = :orderId AND p.status = com.example.flowershop.entity.enums.PaymentStatus.PENDING")
    int failPendingByOrderId(@Param("orderId") String orderId, @Param("actor") String actor);

    @Query("SELECT p FROM Payment p WHERE p.order.customer.id = :customerId AND p.status IN :statuses "
            + "AND p.createdDate >= :start AND p.createdDate < :end")
    org.springframework.data.domain.Page<Payment> findForCustomer(@Param("customerId") String customerId,
            @Param("statuses") java.util.Collection<com.example.flowershop.entity.enums.PaymentStatus> statuses,
            @Param("start") LocalDateTime start, @Param("end") LocalDateTime end,
            org.springframework.data.domain.Pageable pageable);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.order.customer.id = :customerId "
            + "AND p.status = com.example.flowershop.entity.enums.PaymentStatus.SUCCESS "
            + "AND p.createdDate >= :start AND p.createdDate < :end")
    BigDecimal sumSuccessForCustomer(@Param("customerId") String customerId,
            @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /** Mỗi dòng: [orderId, tổng đã thu thành công của đơn]. */
    @Query("SELECT p.order.id, SUM(p.amount) FROM Payment p WHERE p.order.id IN :orderIds "
            + "AND p.status = com.example.flowershop.entity.enums.PaymentStatus.SUCCESS GROUP BY p.order.id")
    List<Object[]> sumSuccessByOrderIds(@Param("orderIds") java.util.Collection<String> orderIds);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.order.shop.id = :shopId "
            + "AND p.status = com.example.flowershop.entity.enums.PaymentStatus.SUCCESS "
            + "AND p.lastModifyDate >= :start AND p.lastModifyDate < :end")
    BigDecimal sumSuccessfulAmountForReport(@Param("shopId") String shopId,
            @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
