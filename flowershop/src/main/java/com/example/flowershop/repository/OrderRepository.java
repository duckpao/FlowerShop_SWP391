package com.example.flowershop.repository;

import com.example.flowershop.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, String> {

    List<Order> findByCustomerIdOrderByCreatedDateDesc(String customerId);

    boolean existsByDeliveryAddressId(String addressId);
}
