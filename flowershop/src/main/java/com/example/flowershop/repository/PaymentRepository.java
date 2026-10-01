package com.example.flowershop.repository;

import com.example.flowershop.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.invoiceNumber = :invoiceNumber")
    Optional<Payment> findByInvoiceNumberForUpdate(@Param("invoiceNumber") String invoiceNumber);

    List<Payment> findByOrderId(String orderId);

    Optional<Payment> findTopByOrderIdOrderByCreatedDateDesc(String orderId);

    Optional<Payment> findByGatewayTransactionNo(String gatewayTransactionNo);

    boolean existsByGatewayTransactionNo(String gatewayTransactionNo);

    Optional<Payment> findByInvoiceNumber(String invoiceNumber);

    boolean existsByInvoiceNumber(String invoiceNumber);
}
