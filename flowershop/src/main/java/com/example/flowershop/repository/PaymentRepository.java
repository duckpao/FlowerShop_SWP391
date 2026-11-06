package com.example.flowershop.repository;

import com.example.flowershop.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, String> {
    List<Payment> findByOrderId(String orderId);

    Optional<Payment> findTopByOrderIdOrderByCreatedDateDesc(String orderId);

    Optional<Payment> findByInvoiceNumber(String invoiceNumber);

    List<Payment> findByGroupInvoice(String groupInvoice);

    /** Lịch sử giao dịch của khách; nạp sẵn order + shop để khỏi N+1 khi map tên shop. */
    @EntityGraph(attributePaths = {"order", "order.shop"})
    Page<Payment> findByOrderCustomerId(String customerId, Pageable pageable);

    /** Cập nhật thẳng trong DB để không ghi đè SUCCESS mà IPN vừa commit. */
    @Modifying
    @Query("UPDATE Payment p SET p.status = com.example.flowershop.entity.enums.PaymentStatus.FAILED, p.lastModifyBy = :actor "
            + "WHERE p.order.id = :orderId AND p.status = com.example.flowershop.entity.enums.PaymentStatus.PENDING")
    int failPendingByOrderId(@Param("orderId") String orderId, @Param("actor") String actor);
}
