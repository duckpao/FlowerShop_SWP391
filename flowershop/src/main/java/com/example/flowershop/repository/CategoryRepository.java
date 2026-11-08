package com.example.flowershop.repository;

import java.util.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.example.flowershop.entity.Category;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Sort;
import java.util.*;
import com.example.flowershop.entity.enums.CategoryStatus;

/** Explicit Hibernate queries; Spring does not generate this repository. */
@Repository
public class CategoryRepository extends HibernateRepository<Category> {
    public CategoryRepository() { super(Category.class); }

    public List<Category> findByStatusOrderByNameAsc(CategoryStatus status) {
        return list("e.status = :status", Map.of("status", status), Sort.by("name"));
    }
public List<Category> findByGroupName(String groupName) { return list("e.groupName = :groupName", Map.of("groupName",groupName)); }
}
