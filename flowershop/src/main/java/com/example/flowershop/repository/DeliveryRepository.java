package com.example.flowershop.repository;

import com.example.flowershop.entity.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DeliveryRepository extends JpaRepository<Delivery, String> {
    Optional<Delivery> findByOrderId(String orderId);

    /** Vận đơn GHN đã delivered từ trước mốc cutoff mà đơn vẫn DELIVERING và không vướng hoàn tiền đang xử lý. */
    @Query("""
            SELECT d FROM Delivery d JOIN d.order o
            WHERE d.ghnStatus = 'delivered' AND d.lastModifyDate < :cutoff
              AND o.status = com.example.flowershop.entity.enums.OrderStatus.DELIVERING
              AND NOT EXISTS (SELECT r.id FROM Refund r WHERE r.order = o
                  AND r.status NOT IN (com.example.flowershop.entity.enums.RefundStatus.REJECTED,
                                       com.example.flowershop.entity.enums.RefundStatus.CANCELLED))
            """)
    List<Delivery> deliveredBeforeWithoutRefund(@Param("cutoff") LocalDateTime cutoff);
}
