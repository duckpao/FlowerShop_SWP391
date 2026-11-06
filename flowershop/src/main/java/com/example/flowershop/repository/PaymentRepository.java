package com.example.flowershop.repository;

import com.example.flowershop.entity.Payment;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Sort;
import java.util.*;

/** Explicit Hibernate queries; Spring does not generate this repository. */
@Repository
public class PaymentRepository extends HibernateRepository<Payment> {
    public PaymentRepository() { super(Payment.class); }

    @Transactional
    public Optional<Payment> findByInvoiceNumberForUpdate(String invoiceNumber) {
        return locked("e.invoiceNumber = :invoiceNumber", Map.of("invoiceNumber", invoiceNumber));
    }

    public List<Payment> findByOrderId(String orderId) {
        return list("e.order.id = :orderId", Map.of("orderId", orderId));
    }

    public Optional<Payment> findTopByOrderIdOrderByCreatedDateDesc(String orderId) {
        return first("e.order.id = :orderId", Map.of("orderId", orderId), Sort.by(Sort.Order.desc("createdDate"), Sort.Order.asc("id")));
    }

    public Optional<Payment> findByGatewayTransactionNo(String gatewayTransactionNo) {
        return one("e.gatewayTransactionNo = :value", Collections.singletonMap("value", gatewayTransactionNo));
    }

    public boolean existsByGatewayTransactionNo(String gatewayTransactionNo) {
        return countWhere("e.gatewayTransactionNo = :value", Collections.singletonMap("value", gatewayTransactionNo)) > 0;
    }

    public Optional<Payment> findByInvoiceNumber(String invoiceNumber) {
        return one("e.invoiceNumber = :value", Collections.singletonMap("value", invoiceNumber));
    }

    public boolean existsByInvoiceNumber(String invoiceNumber) {
        return countWhere("e.invoiceNumber = :value", Collections.singletonMap("value", invoiceNumber)) > 0;
    }
}
