package com.example.flowershop.repository;

import com.example.flowershop.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ChatSessionRepository extends JpaRepository<ChatSession, String> {

    Optional<ChatSession> findByCustomerIdAndShopId(String customerId, String shopId);

    @Query("select s from ChatSession s join fetch s.shop join fetch s.customer where s.customer.id = :customerId "
            + "order by s.lastMessageAt desc nulls last, s.createdDate desc")
    List<ChatSession> forCustomer(String customerId);

    @Query("select s from ChatSession s join fetch s.shop join fetch s.customer where s.shop.id = :shopId "
            + "and s.lastMessageAt is not null order by s.lastMessageAt desc")
    List<ChatSession> forShop(String shopId);

    @Query("select coalesce(sum(s.customerUnreadCount), 0) from ChatSession s where s.customer.id = :customerId")
    long customerUnreadTotal(String customerId);

    @Query("select coalesce(sum(s.shopUnreadCount), 0) from ChatSession s where s.shop.id = :shopId")
    long shopUnreadTotal(String shopId);
}
