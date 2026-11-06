package com.example.flowershop.repository;

import com.example.flowershop.entity.CustomOrderRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.*;
import com.example.flowershop.entity.enums.CustomOrderStatus;

/** Explicit Hibernate queries; Spring does not generate this repository. */
@Repository
public class CustomOrderRequestRepository extends HibernateRepository<CustomOrderRequest> {
    public CustomOrderRequestRepository() { super(CustomOrderRequest.class); }

    public Page<CustomOrderRequest> findByCustomerId(String customerId, Pageable pageable) {
        return page("e.customer.id = :customerId", Map.of("customerId", customerId), pageable);
    }

    public Page<CustomOrderRequest> findByShopId(String shopId, Pageable pageable) {
        return page("e.shop.id = :shopId", Map.of("shopId", shopId), pageable);
    }

    public Page<CustomOrderRequest> findByShopIdAndStatus(String shopId, CustomOrderStatus status, Pageable pageable) {
        return page("e.shop.id = :shopId and e.status = :status", Map.of("shopId", shopId, "status", status), pageable);
    }

    public Optional<CustomOrderRequest> findByIdAndCustomerId(String id, String customerId) {
        return one("e.id = :id and e.customer.id = :customerId", Map.of("id", id, "customerId", customerId));
    }

    @Transactional
    public Optional<CustomOrderRequest> findLocked(String id) {
        return locked("e.id = :id", Map.of("id", id));
    }
}
