package com.example.flowershop.repository;

import java.util.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.example.flowershop.entity.Order;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.util.*;
import com.example.flowershop.entity.enums.OrderStatus;
import java.time.LocalDateTime;

/** Explicit Hibernate queries; Spring does not generate this repository. */
@Repository
public class OrderRepository extends HibernateRepository<Order> {
    public Optional<Order> findLinkedCustomOrder(String referenceId,String customerId,String shopId) {
        return first("(e.id = :referenceId or e.customRequestId = :referenceId) and e.orderType = com.example.flowershop.entity.enums.OrderType.CUSTOM and e.customer.id = :customerId and e.shop.id = :shopId",
                Map.of("referenceId",referenceId,"customerId",customerId,"shopId",shopId),Sort.by(Sort.Order.desc("createdDate"),Sort.Order.asc("id")));
    }
    public OrderRepository() { super(Order.class); }

    @Transactional
    public Optional<Order> findByIdForUpdate(String id) {
        return locked("e.id = :id", Map.of("id", id));
    }

    public List<Order> findByCustomerIdOrderByCreatedDateDesc(String customerId) {
        return list("e.customer.id = :customerId", Map.of("customerId", customerId), Sort.by(Sort.Order.desc("createdDate"), Sort.Order.asc("id")));
    }

    public List<Order> findByStatusAndCreatedDateBefore(OrderStatus status, LocalDateTime threshold) {
        return list("e.status = :status and e.createdDate < :threshold", Map.of("status", status, "threshold", threshold));
    }

    public boolean existsByDeliveryAddressId(String addressId) {
        return countWhere("e.deliveryAddress.id = :addressId", Map.of("addressId", addressId)) > 0;
    }

    public Page<Order> findByShopIdAndStatusIn(String shopId, Collection<OrderStatus> statuses, Pageable pageable) {
        return page("e.shop.id = :shopId and e.status in :statuses", Map.of("shopId", shopId, "statuses", statuses), pageable);
    }

    public long countByShopIdAndStatusIn(String shopId, Collection<OrderStatus> statuses) {
        return countWhere("e.shop.id = :shopId and e.status in :statuses", Map.of("shopId", shopId, "statuses", statuses));
    }

    public Page<Order> findByShopId(String shopId, Pageable pageable) {
        return page("e.shop.id = :shopId", Map.of("shopId", shopId), pageable);
    }

    public Page<Order> findByShopIdAndStatus(String shopId, OrderStatus status, Pageable pageable) {
        return page("e.shop.id = :shopId and e.status = :status", Map.of("shopId", shopId, "status", status), pageable);
    }

    public Optional<Order> findByIdAndShopId(String id, String shopId) {
        return one("e.id = :id and e.shop.id = :shopId", Map.of("id", id, "shopId", shopId));
    }

    public Optional<Order> findByIdAndCustomerId(String id, String customerId) {
        return one("e.id = :id and e.customer.id = :customerId", Map.of("id", id, "customerId", customerId));
    }

    public Page<Order> findByCustomerIdOrderByCreatedDateDesc(String customerId, Pageable pageable) {
        return page("e.customer.id = :customerId", Map.of("customerId", customerId), pageable);
    }

    public Page<Order> findByCustomerIdAndStatus(String customerId, OrderStatus status, Pageable pageable) {
        return page("e.customer.id = :customerId and e.status = :status", Map.of("customerId", customerId, "status", status), pageable);
    }

    public List<Order> findByCustomerIdAndStatusOrderByCreatedDateDesc(String customerId, OrderStatus status) {
        return list("e.customer.id = :customerId and e.status = :status", Map.of("customerId", customerId, "status", status), Sort.by(Sort.Order.desc("createdDate"), Sort.Order.asc("id")));
    }
public boolean existsByCustomRequestIdAndStatusNot(String customRequestId, OrderStatus status) { return countWhere("e.customRequestId = :id and e.status <> :status", Map.of("id",customRequestId,"status",status)) > 0; }
public Page<Order> findByCustomerIdAndStatusOrderByCreatedDateDesc(String customerId, OrderStatus status, Pageable pageable) { return findByCustomerIdAndStatus(customerId,status,pageable); }
public List<Object[]> countStatusesForReport(String shopId, LocalDateTime start, LocalDateTime end) { return session().createSelectionQuery("SELECT o.status, COUNT(o) FROM Order o WHERE o.shop.id = :shopId AND o.createdDate >= :start AND o.createdDate < :end GROUP BY o.status",Object[].class).setParameter("shopId",shopId).setParameter("start",start).setParameter("end",end).getResultList(); }

}
