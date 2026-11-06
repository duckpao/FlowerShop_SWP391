package com.example.flowershop.repository;

import com.example.flowershop.entity.Address;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Sort;
import java.util.*;

/** Explicit Hibernate queries; Spring does not generate this repository. */
@Repository
public class AddressRepository extends HibernateRepository<Address> {
    public AddressRepository() { super(Address.class); }

    public List<Address> findByUserId(String userId) {
        return list("e.user.id = :userId", Map.of("userId", userId));
    }

    public Optional<Address> findByUserIdAndIsDefaultTrue(String userId) {
        return one("e.user.id = :userId and e.isDefault = true", Map.of("userId", userId));
    }

    public List<Address> findByShopIdAndUserIsNullOrderByCreatedDateAscIdAsc(String shopId) {
        return list("e.shop.id = :shopId and e.user is null", Map.of("shopId", shopId), Sort.by("createdDate", "id"));
    }

    public List<Address> findByUserIdAndShopIsNullOrderByCreatedDateAscIdAsc(String userId) {
        return list("e.user.id = :userId and e.shop is null", Map.of("userId", userId), Sort.by("createdDate", "id"));
    }

    public Optional<Address> findByIdAndUserId(String id, String userId) {
        return one("e.id = :id and e.user.id = :userId", Map.of("id", id, "userId", userId));
    }

    public Optional<Address> findPickupByShopId(String shopId) {
        return session().createSelectionQuery("from Address e where e.shop.id = :shopId and e.user is null "
                + "order by case when e.isDefault = true then 0 else 1 end, e.createdDate asc, e.id asc", Address.class)
                .setParameter("shopId", shopId).setMaxResults(1).uniqueResultOptional();
    }
}
