package com.irum.productservice.domain.product.domain.repository;

import com.irum.productservice.domain.product.domain.entity.Product;
import com.irum.productservice.domain.product.domain.entity.ProductOptionGroup;
import com.irum.productservice.domain.product.domain.entity.ProductOptionValue;
import feign.Param;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductOptionValueRepository extends JpaRepository<ProductOptionValue, UUID> {

    /** 락 획득까지 최대 3초 대기 TODO : 대기 시간 정책 정하기 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")})
    @Query("select pov from ProductOptionValue pov where pov.id in :ids")
    List<ProductOptionValue> findAllByIdInWithLock(@Param("ids") List<UUID> ids);

    @Query("select pov from ProductOptionValue pov where pov.id in :ids")
    List<ProductOptionValue> findAllByIds(@Param("ids") List<UUID> ids);
    List<ProductOptionValue> findAllByOptionGroup(ProductOptionGroup optionGroup);

    List<ProductOptionValue> findAllByOptionGroup_Product(Product product);

    /** 기본적으로 fetch join은 락에 안잡힘 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")})
    @Query(
            """
    SELECT pov
    FROM ProductOptionValue pov
    JOIN FETCH pov.optionGroup og
    JOIN FETCH og.product p
    JOIN FETCH p.store s
    WHERE pov.id IN :ids
    """)
    List<ProductOptionValue> findAllByIdWithLockFetchJoin(@Param("ids") List<UUID> ids);

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
