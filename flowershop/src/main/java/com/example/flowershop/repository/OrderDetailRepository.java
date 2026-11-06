package com.example.flowershop.repository;

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
}
