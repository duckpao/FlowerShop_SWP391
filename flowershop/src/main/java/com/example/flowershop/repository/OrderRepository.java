package com.example.flowershop.repository;

import com.example.flowershop.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") String id);

    List<Order> findByCustomerIdOrderByCreatedDateDesc(String customerId);

    List<Order> findByStatusAndCreatedDateBefore(com.example.flowershop.entity.enums.OrderStatus status, LocalDateTime threshold);

    boolean existsByDeliveryAddressId(String addressId);
}
