package com.example.flowershop.repository;
import com.example.flowershop.entity.AuthSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface AuthSessionRepository extends JpaRepository<AuthSession, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from AuthSession s where s.id = :id")
    Optional<AuthSession> findLocked(@Param("id") String id);
    @Modifying
    @Query("update AuthSession s set s.revoked = true where s.userId = :userId")
    int revokeAll(@Param("userId") String userId);
}
