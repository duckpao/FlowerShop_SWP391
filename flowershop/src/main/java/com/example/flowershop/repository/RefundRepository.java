package com.example.flowershop.repository;

import com.example.flowershop.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RefundRepository extends JpaRepository<Refund, String> {
    List<Refund> findByOrderShopIdOrderByCreatedDateDesc(String shopId);

    List<Refund> findByOrderIdOrderByCreatedDateAsc(String orderId);
}
