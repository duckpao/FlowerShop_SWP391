package com.example.flowershop.repository;

import com.example.flowershop.entity.Delivery;
import org.springframework.stereotype.Repository;
import java.util.*;

/** Explicit Hibernate queries; Spring does not generate this repository. */
@Repository
public class DeliveryRepository extends HibernateRepository<Delivery> {
    public DeliveryRepository() { super(Delivery.class); }

    public Optional<Delivery> findByOrderId(String orderId) {
        return one("e.order.id = :orderId", Map.of("orderId", orderId));
    }
}
