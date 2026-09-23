package com.example.flowershop.repository;
import com.example.flowershop.entity.ShopStaff;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ShopStaffRepository extends JpaRepository<ShopStaff,String> {
    List<ShopStaff> findByShopIdOrderByIdAsc(String shopId);
    List<ShopStaff> findByUserIdAndActiveTrue(String userId);
    Optional<ShopStaff> findByShopIdAndUserId(String shopId,String userId);
}
