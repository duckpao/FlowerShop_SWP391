package com.example.flowershop.repository;
import com.example.flowershop.entity.StaffInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
import java.time.Instant;
public interface StaffInvitationRepository extends JpaRepository<StaffInvitation,String> {
    @org.springframework.data.jpa.repository.Query("select i.shop.id from StaffInvitation i where i.id=:id")
    Optional<String> findShopId(@org.springframework.data.repository.query.Param("id") String id);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    Optional<StaffInvitation> findByShopIdAndEmail(String shopId,String email);
    List<StaffInvitation> findByShopIdOrderByIssuedAtDesc(String shopId);
    long countByShopIdAndIssuedAtAfter(String shopId,Instant after);
}
