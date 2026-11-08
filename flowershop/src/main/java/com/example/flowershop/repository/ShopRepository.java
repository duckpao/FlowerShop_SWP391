package com.example.flowershop.repository;

import com.example.flowershop.entity.Shop;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.util.*;
import com.example.flowershop.entity.enums.ShopStatus;

/** Explicit Hibernate queries; Spring does not generate this repository. */
@Repository
public class ShopRepository extends HibernateRepository<Shop> {
    public ShopRepository() { super(Shop.class); }

    public List<Shop> findByStatusAndNameContainingIgnoreCaseOrderByNameAsc(ShopStatus status, String name, Pageable pageable) {
        String escaped = name.toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_");
        var query = selection("e.status = :status and lower(e.name) like :name escape '!'",
                Map.of("status", status, "name", "%" + escaped + "%"), Sort.by("name"));
        if (pageable.isPaged()) query.setFirstResult(Math.toIntExact(pageable.getOffset())).setMaxResults(pageable.getPageSize());
        return query.getResultList();
    }

    public List<Shop> findByOwnerIdOrderByNameAsc(String ownerId) {
        return list("e.owner.id = :ownerId", Map.of("ownerId", ownerId), Sort.by("name"));
    }

    @Transactional
    public Optional<Shop> findForUpdate(String id) {
        return locked("e.id = :id", Map.of("id", id));
    }

    public Optional<Shop> findByIdAndStatus(String id, ShopStatus status) {
        return one("e.id = :id and e.status = :status", Map.of("id", id, "status", status));
    }
}
