package com.example.flowershop.repository;
import com.example.flowershop.entity.StaffApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface StaffApplicationRepository extends JpaRepository<StaffApplication,String> {
    Optional<StaffApplication> findByShopIdAndUserId(String shopId,String userId);
    List<StaffApplication> findByShopIdOrderBySubmittedAtDesc(String shopId);
    List<StaffApplication> findByUserIdOrderBySubmittedAtDesc(String userId);
}
