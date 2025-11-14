package com.irum.productservice.domain.cart.domain.repository;

import com.irum.productservice.domain.cart.domain.entity.CartRedis;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

public interface CartRedisRepository extends CrudRepository<CartRedis, String> {

    List<CartRedis> findByMemberId(Long memberId);
}
