package com.example.flowershop.repository;

import com.example.flowershop.entity.Shop;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface ShopRepository extends JpaRepository<Shop,String>, JpaSpecificationExecutor<Shop> {
    java.util.List<Shop> findByStatusAndNameContainingIgnoreCaseOrderByNameAsc(com.example.flowershop.entity.enums.ShopStatus status,String name,org.springframework.data.domain.Pageable pageable);
    // ManagerShopService.mine lấy shop của tài khoản để FE xác định shopId cho trang quản lý sản phẩm.
    java.util.List<Shop> findByOwnerIdOrderByNameAsc(String ownerId);
    // Product Management/reply review khóa shop khi ghi để kiểm tra trạng thái/chủ shop trong transaction.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Shop s where s.id=:id")
    Optional<Shop> findForUpdate(@Param("id") String id);
}
