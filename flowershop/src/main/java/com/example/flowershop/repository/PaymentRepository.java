package com.example.flowershop.repository;

import com.example.flowershop.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, String> {

    List<Payment> findByOrderId(String orderId);

    Optional<Payment> findTopByOrderIdOrderByCreatedDateDesc(String orderId);

    Optional<Payment> findByGatewayTransactionNo(String gatewayTransactionNo);

    boolean existsByGatewayTransactionNo(String gatewayTransactionNo);

    Optional<Payment> findByInvoiceNumber(String invoiceNumber);

    boolean existsByInvoiceNumber(String invoiceNumber);
}

