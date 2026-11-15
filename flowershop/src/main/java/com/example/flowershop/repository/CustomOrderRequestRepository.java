package com.example.flowershop.repository;

import com.example.flowershop.entity.CustomOrderRequest;
import com.example.flowershop.entity.enums.CustomOrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface CustomOrderRequestRepository extends JpaRepository<CustomOrderRequest,String> {
    Page<CustomOrderRequest> findByCustomerId(String customerId, Pageable page);
    Page<CustomOrderRequest> findByShopId(String shopId, Pageable page);
    Page<CustomOrderRequest> findByShopIdAndStatus(String shopId, CustomOrderStatus status, Pageable page);
    Optional<CustomOrderRequest> findByIdAndCustomerId(String id, String customerId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from CustomOrderRequest r where r.id=:id")
    Optional<CustomOrderRequest> findLocked(@Param("id") String id);
}
