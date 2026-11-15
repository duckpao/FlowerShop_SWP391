package com.example.flowershop.repository;

import com.example.flowershop.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, String> {

    List<CartItem> findByUserId(String userId);

    Optional<CartItem> findByUserIdAndProductId(String userId, String productId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT item FROM CartItem item WHERE item.user.id = :userId AND item.product.id = :productId")
    Optional<CartItem> findByUserIdAndProductIdForUpdate(@Param("userId") String userId, @Param("productId") String productId);

    /** Xóa dòng giỏ không đổi từ trước mốc cutoff, trả về số dòng đã xóa. */
    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM CartItem item WHERE item.lastModifyDate < :cutoff")
    int deleteNotModifiedSince(@Param("cutoff") java.time.LocalDateTime cutoff);
}
