package com.example.flowershop.repository;
import com.example.flowershop.entity.ShopProductMail;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
public interface ShopProductMailRepository extends JpaRepository<ShopProductMail, String> {
    @Query("select m.id from ShopProductMail m where m.status = 'PENDING' and m.nextAttemptAt <= :now order by m.createdAt, m.id")
    List<String> due(@Param("now") LocalDateTime now, Pageable page);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from ShopProductMail m where m.id = :id")
    Optional<ShopProductMail> findLocked(@Param("id") String id);
}
