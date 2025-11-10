package com.irum.productservice.domain.deliverypolicy.domain.repository;

import com.irum.productservice.domain.deliverypolicy.domain.entity.DeliveryPolicy;
import com.irum.productservice.domain.product.domain.entity.Product;
import com.irum.productservice.domain.store.domain.entity.Store;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryPolicyRepository extends JpaRepository<DeliveryPolicy, UUID> {

    boolean existsByStore(Store store);
    Optional<DeliveryPolicy> findByStoreId(UUID storeId);

}
