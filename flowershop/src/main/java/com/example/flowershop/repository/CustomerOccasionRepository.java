package com.example.flowershop.repository;

import com.example.flowershop.entity.CustomerOccasion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CustomerOccasionRepository extends JpaRepository<CustomerOccasion, String> {

    List<CustomerOccasion> findByCustomerIdOrderByOccasionDateAsc(String customerId);

    Optional<CustomerOccasion> findByIdAndCustomerId(String id, String customerId);

    long countByCustomerId(String customerId);

    /** Dịp còn có thể cần nhắc: mọi dịp hằng năm, và dịp một lần chưa qua ngày mà chưa nhắc. */
    @Query("select o from CustomerOccasion o join fetch o.customer where o.recurringYearly = true "
            + "or (o.occasionDate >= :today and (o.reminderSent is null or o.reminderSent = false))")
    List<CustomerOccasion> findReminderCandidates(LocalDate today);
}
