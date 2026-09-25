package com.example.flowershop.repository;

import com.example.flowershop.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, String> {

    List<Address> findByUserId(String userId);

    Optional<Address> findByUserIdAndIsDefaultTrue(String userId);

    List<Address> findByShopIdAndUserIsNullOrderByCreatedDateAscIdAsc(String shopId);

    List<Address> findByUserIdAndShopIsNullOrderByCreatedDateAscIdAsc(String userId);
}
