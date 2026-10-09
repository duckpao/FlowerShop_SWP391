package com.example.flowershop.repository;

import com.example.flowershop.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String>, JpaSpecificationExecutor<User> {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    @Query("select u.id from User u where u.email = :email")
    Optional<String> findUserIdByEmail(@Param("email") String email);

    @Query(value = "select * from Users where id = :id for update", nativeQuery = true)
    Optional<User> findByIdForUpdate(@Param("id") String id);
}
