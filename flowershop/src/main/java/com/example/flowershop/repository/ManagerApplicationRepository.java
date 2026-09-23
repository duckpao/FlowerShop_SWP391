package com.example.flowershop.repository;
import com.example.flowershop.entity.ManagerApplication;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.util.Optional;
public interface ManagerApplicationRepository extends JpaRepository<ManagerApplication,String> {
    Optional<ManagerApplication> findByUserId(String userId);
    @Query("select a.user.id from ManagerApplication a where a.id=:id")
    Optional<String> applicantId(@Param("id") String id);
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from ManagerApplication a where a.id=:id")
    Optional<ManagerApplication> lock(@Param("id") String id);
    Page<ManagerApplication> findByStatus(String status,Pageable pageable);
}
