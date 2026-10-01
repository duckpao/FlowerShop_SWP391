package com.example.flowershop.repository;

import com.example.flowershop.entity.Order;
import com.example.flowershop.entity.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, String> {
    boolean existsByDeliveryAddressId(String addressId);
    org.springframework.data.domain.Page<Order> findByShopIdAndStatusIn(String shopId, java.util.Collection<com.example.flowershop.entity.enums.OrderStatus> statuses, org.springframework.data.domain.Pageable pageable);
    long countByShopIdAndStatusIn(String shopId, java.util.Collection<com.example.flowershop.entity.enums.OrderStatus> statuses);
    Page<Order> findByShopId(String shopId, Pageable pageable);
    Page<Order> findByShopIdAndStatus(String shopId, OrderStatus status, Pageable pageable);
    java.util.Optional<Order> findByIdAndShopId(String id, String shopId);
    Page<Order> findByCustomerIdOrderByCreatedDateDesc(String customerId, Pageable pageable);
    Page<Order> findByShopIdAndStatusIn(String shopId, java.util.List<OrderStatus> statuses, Pageable pageable);
    long countByShopIdAndStatusIn(String shopId, java.util.List<OrderStatus> statuses);
}
