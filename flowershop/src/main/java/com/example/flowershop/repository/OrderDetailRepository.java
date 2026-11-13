package com.example.flowershop.repository;

import java.util.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.example.flowershop.entity.OrderDetail;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Sort;
import java.util.*;

/** Explicit Hibernate queries; Spring does not generate this repository. */
@Repository
public class OrderDetailRepository extends HibernateRepository<OrderDetail> {
    public OrderDetailRepository() { super(OrderDetail.class); }

    public List<OrderDetail> findByOrderId(String orderId) {
        return list("e.order.id = :orderId", Map.of("orderId", orderId));
    }

    public List<OrderDetail> findByOrderIdIn(Collection<String> orderIds) {
        if (orderIds.isEmpty()) return List.of(); return list("e.order.id in :orderIds", Map.of("orderIds", orderIds));
    }

    public Optional<OrderDetail> findCustomDesignByOrderId(String orderId) {
        return first("e.order.id = :orderId and e.customMaterials is not null", Map.of("orderId", orderId), Sort.by("id"));
    }

    public Map<String, Long> countByOrderIds(Collection<String> orderIds) {
        if (orderIds.isEmpty()) return Map.of();
        var rows = session().createSelectionQuery(
                "select e.order.id, count(e) from OrderDetail e where e.order.id in :ids group by e.order.id", Object[].class)
                .setParameterList("ids", orderIds).getResultList();
        Map<String, Long> counts = new HashMap<>();
        for (Object[] row : rows) counts.put((String) row[0], ((Number) row[1]).longValue());
        return counts;
    }

public List<Object[]> productPerformanceForReport(String shopId, LocalDateTime start, LocalDateTime end) { return session().createSelectionQuery("SELECT d.product.id, d.product.name, SUM(CASE WHEN d.order.status = com.example.flowershop.entity.enums.OrderStatus.COMPLETED THEN d.quantity ELSE 0 END), SUM(CASE WHEN d.order.status = com.example.flowershop.entity.enums.OrderStatus.REFUNDED THEN d.quantity ELSE 0 END), COUNT(DISTINCT CASE WHEN d.order.status = com.example.flowershop.entity.enums.OrderStatus.COMPLETED THEN d.order.id ELSE NULL END), COUNT(DISTINCT CASE WHEN d.order.status = com.example.flowershop.entity.enums.OrderStatus.REFUNDED THEN d.order.id ELSE NULL END) FROM OrderDetail d WHERE d.order.shop.id = :shopId AND d.product IS NOT NULL AND d.order.createdDate >= :start AND d.order.createdDate < :end AND d.order.status IN (com.example.flowershop.entity.enums.OrderStatus.COMPLETED, com.example.flowershop.entity.enums.OrderStatus.REFUNDED) GROUP BY d.product.id, d.product.name ORDER BY d.product.name",Object[].class).setParameter("shopId",shopId).setParameter("start",start).setParameter("end",end).getResultList(); }

}
