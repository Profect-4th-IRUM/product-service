package com.irum.productservice.domain.product.domain.repository;

import com.irum.productservice.domain.product.domain.entity.Product;
import com.irum.productservice.domain.product.domain.entity.ProductOptionGroup;
import com.irum.productservice.domain.product.domain.entity.ProductOptionValue;
import feign.Param;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductOptionValueRepository extends JpaRepository<ProductOptionValue, UUID> {

    @Query("select pov from ProductOptionValue pov where pov.id in :ids")
    List<ProductOptionValue> findAllByIds(@Param("ids") List<UUID> ids);

    List<ProductOptionValue> findAllByOptionGroup(ProductOptionGroup optionGroup);

    List<ProductOptionValue> findAllByOptionGroup_Product(Product product);

    List<ProductOptionValue> findAllByOptionGroup_Id(UUID optionGroupId);

    @Query(
            """
    SELECT pov
    FROM ProductOptionValue pov
    JOIN FETCH pov.optionGroup og
    JOIN FETCH og.product p
    JOIN FETCH p.store s
    WHERE pov.id IN :ids
    """)
    List<ProductOptionValue> findAllByIdWithFetchJoin(@Param("ids") List<UUID> ids);
}
