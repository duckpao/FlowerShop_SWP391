package com.example.flowershop.repository;

import com.example.flowershop.entity.PendingRegistration;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface PendingRegistrationRepository extends JpaRepository<PendingRegistration, String> {
    @Query(value = "select * from Pending_Registrations where email = :email for update", nativeQuery = true)
    Optional<PendingRegistration> findLocked(@Param("email") String email);
}
