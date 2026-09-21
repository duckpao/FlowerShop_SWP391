package com.example.flowershop.repository;

import com.example.flowershop.entity.Shop;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface ShopRepository extends JpaRepository<Shop,String>, JpaSpecificationExecutor<Shop> {
    java.util.List<Shop> findByStatusAndNameContainingIgnoreCaseOrderByNameAsc(com.example.flowershop.entity.enums.ShopStatus status,String name,org.springframework.data.domain.Pageable pageable);
    java.util.List<Shop> findByOwnerIdOrderByNameAsc(String ownerId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Shop s where s.id=:id")
    Optional<Shop> findForUpdate(@Param("id") String id);
}
