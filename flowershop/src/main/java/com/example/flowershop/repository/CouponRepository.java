package com.example.flowershop.repository;

import com.example.flowershop.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponRepository extends JpaRepository<Coupon, String> {
    Optional<Coupon> findByCodeIgnoreCase(String code);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Coupon c WHERE LOWER(c.code) = LOWER(:code) AND c.deleted = false")
    Optional<Coupon> findByCodeForUpdate(@Param("code") String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Coupon c WHERE c.id = :id")
    Optional<Coupon> findLocked(@Param("id") String id);

    /** Voucher sàn (shop null) chưa xóa, mới nhất trước. */
    @Query("SELECT c FROM Coupon c WHERE c.deleted = false AND c.shop IS NULL ORDER BY c.createdDate DESC, c.code")
    List<Coupon> managedPlatform();

    /** Voucher chưa xóa của một shop, mới nhất trước. */
    @Query("SELECT c FROM Coupon c WHERE c.deleted = false AND c.shop.id = :shopId ORDER BY c.createdDate DESC, c.code")
    List<Coupon> managedByShop(@Param("shopId") String shopId);

    /** Voucher còn bật mà một đơn của shop này có thể xét: voucher sàn và voucher của chính shop. */
    @Query("SELECT c FROM Coupon c WHERE c.deleted = false AND c.status = com.example.flowershop.entity.enums.CouponStatus.ACTIVE AND (c.shop IS NULL OR c.shop.id = :shopId) ORDER BY c.code")
    List<Coupon> available(@Param("shopId") String shopId);

    /** Voucher còn ACTIVE, chưa xóa mà đã quá ngày kết thúc. */
    @Query("SELECT c FROM Coupon c WHERE c.deleted = false AND c.status = com.example.flowershop.entity.enums.CouponStatus.ACTIVE AND c.endDate < :now")
    List<Coupon> activeEndedBefore(@Param("now") java.time.LocalDateTime now);
}
