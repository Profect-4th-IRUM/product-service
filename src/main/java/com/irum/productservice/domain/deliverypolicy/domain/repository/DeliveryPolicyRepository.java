package com.irum.productservice.domain.deliverypolicy.domain.repository;

import com.irum.productservice.domain.deliverypolicy.domain.entity.DeliveryPolicy;
import com.irum.productservice.domain.store.domain.entity.Store;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryPolicyRepository extends JpaRepository<DeliveryPolicy, UUID> {

    boolean existsByStore(Store store);

    Optional<DeliveryPolicy> findByStoreId(UUID storeId);
}
