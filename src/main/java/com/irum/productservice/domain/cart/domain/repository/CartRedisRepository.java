package com.irum.productservice.domain.cart.domain.repository;

import com.irum.productservice.domain.cart.domain.entity.CartRedis;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;

public interface CartRedisRepository extends CrudRepository<CartRedis, String> {

    List<CartRedis> findByMemberId(Long memberId);

    Optional<CartRedis> findByMemberIdAndCartId(Long memberId, UUID cartId);

    long deleteByMemberIdAndCartId(Long memberId, UUID cartId);
}
