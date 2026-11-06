package com.example.flowershop.repository;

import com.example.flowershop.entity.User;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import com.example.flowershop.entity.enums.UserRole;
import com.example.flowershop.entity.enums.UserStatus;

/** Explicit Hibernate queries; Spring does not generate this repository. */
@Repository
public class UserRepository extends HibernateRepository<User> {
    public UserRepository() { super(User.class); }

    public boolean existsByEmail(String email) {
        return countWhere("e.email = :email", Map.of("email", email)) > 0;
    }

    public Optional<User> findByEmail(String email) {
        return one("e.email = :email", Map.of("email", email));
    }

    public Optional<String> findUserIdByEmail(String email) {
        return session().createSelectionQuery("select e.id from User e where e.email = :email", String.class).setParameter("email", email).uniqueResultOptional();
    }

    @Transactional
    public Optional<User> findByIdForUpdate(String id) {
        return locked("e.id = :id", Map.of("id", id));
    }

    public Optional<User> findByIdAndRoleAndStatus(String id, UserRole role, UserStatus status) {
        return one("e.id = :id and e.role = :role and e.status = :status", Map.of("id", id, "role", role, "status", status));
    }
}
