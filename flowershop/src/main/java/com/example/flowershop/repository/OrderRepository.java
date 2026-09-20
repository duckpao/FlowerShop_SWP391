package com.example.flowershop.repository;

import com.example.flowershop.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, String> {
    boolean existsByDeliveryAddressId(String addressId);
}
