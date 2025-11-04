package com.irum.productservice.domain.store.domain.repository;

import com.irum.productservice.domain.store.domain.entity.Store;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoreRepository extends JpaRepository<Store, UUID>, StoreRepositoryCustom {

    boolean existsByMember(UUID member);

    boolean existsByBusinessRegistrationNumber(String businessRegistrationNumber);

    boolean existsByTelemarketingRegistrationNumber(String telemarketingRegistrationNumber);

    Optional<Store> findByMember(UUID member);

}
