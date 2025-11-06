package com.irum.productservice.domain.product.domain.repository;

import com.irum.productservice.domain.product.domain.entity.Product;

import java.util.List;
import java.util.UUID;

import com.irum.productservice.domain.product.domain.entity.ProductOptionValue;
import com.irum.productservice.domain.store.domain.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, UUID>, ProductRepositoryCustom {
    List<Product> findByStoreId(UUID storeId);

}
