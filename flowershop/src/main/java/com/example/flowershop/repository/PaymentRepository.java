package com.example.flowershop.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.example.flowershop.entity.Payment;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.*;

/** Explicit Hibernate queries; Spring does not generate this repository. */
@Repository
public class PaymentRepository extends HibernateRepository<Payment> {
    public PaymentRepository() { super(Payment.class); }

    public Page<Payment> findByOrderCustomerId(String customerId, Pageable pageable) {
        return page("e.order.customer.id = :customerId", Map.of("customerId", customerId), pageable);
    }

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
public List<Payment> findByGroupInvoice(String groupInvoice) { return list("e.groupInvoice = :invoice", Map.of("invoice",groupInvoice)); }
@org.springframework.transaction.annotation.Transactional
public int failPendingByOrderId(String orderId, String actor) { return session().createMutationQuery("UPDATE Payment p SET p.status = com.example.flowershop.entity.enums.PaymentStatus.FAILED, p.lastModifyBy = :actor WHERE p.order.id = :orderId AND p.status = com.example.flowershop.entity.enums.PaymentStatus.PENDING").setParameter("orderId",orderId).setParameter("actor",actor).executeUpdate(); }

public BigDecimal sumSuccessfulAmountForReport(String shopId, LocalDateTime start, LocalDateTime end) { return session().createSelectionQuery("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.order.shop.id = :shopId AND p.status = com.example.flowershop.entity.enums.PaymentStatus.SUCCESS AND p.lastModifyDate >= :start AND p.lastModifyDate < :end",BigDecimal.class).setParameter("shopId",shopId).setParameter("start",start).setParameter("end",end).getSingleResult(); }

}
