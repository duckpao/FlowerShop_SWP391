package com.example.flowershop.repository;

import com.example.flowershop.entity.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;

public interface OrderDetailRepository extends JpaRepository<OrderDetail, String> {
    List<OrderDetail> findByOrderId(String orderId);
    List<OrderDetail> findByOrderIdIn(Collection<String> orderIds);
}