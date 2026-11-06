package com.example.flowershop.repository;

import com.example.flowershop.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import java.util.List;

public interface RefundRepository extends JpaRepository<Refund, String> {
    List<Refund> findByOrderShopIdOrderByCreatedDateDesc(String shopId);

    List<Refund> findByOrderIdOrderByCreatedDateAsc(String orderId);

    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Refund r WHERE r.order.shop.id = :shopId "
            + "AND r.status = com.example.flowershop.entity.enums.RefundStatus.PROCESSED "
            + "AND r.lastModifyDate >= :start AND r.lastModifyDate < :end")
    BigDecimal sumProcessedAmountForReport(@Param("shopId") String shopId,
            @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
