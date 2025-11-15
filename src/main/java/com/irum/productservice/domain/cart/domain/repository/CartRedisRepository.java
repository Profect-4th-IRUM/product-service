package com.irum.productservice.domain.cart.domain.repository;

import com.irum.productservice.domain.cart.domain.entity.CartRedis;
import org.springframework.data.repository.CrudRepository;

public interface CartRedisRepository extends CrudRepository<CartRedis, String> {}
