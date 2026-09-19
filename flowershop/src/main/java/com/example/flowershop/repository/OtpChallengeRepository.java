package com.example.flowershop.repository;
import com.example.flowershop.entity.OtpChallenge;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface OtpChallengeRepository extends JpaRepository<OtpChallenge, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from OtpChallenge c where c.userId = :userId and c.purpose = :purpose")
    Optional<OtpChallenge> findLocked(@Param("userId") String userId, @Param("purpose") OtpChallenge.Purpose purpose);
    Optional<OtpChallenge> findByUserIdAndPurpose(String userId, OtpChallenge.Purpose purpose);
}
