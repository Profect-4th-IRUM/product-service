package com.irum.productservice.domain.cart.domain.repository;

import com.irum.productservice.domain.cart.domain.model.CartRedis;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CartRedisRepository {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String PREFIX = "cart:";

    /** Redis Key 생성 */
    private String key(Long memberId) {
        return PREFIX + memberId;
    }

    /** 장바구니 아이템 저장 */
    public void save(CartRedis cart) {
        String key = key(cart.getMemberId());
        redisTemplate.opsForHash().put(key, cart.getCartId().toString(), cart);
        redisTemplate.expire(key, 10, TimeUnit.SECONDS);
    }

    /** 특정 회원의 장바구니 아이템 전체 조회 */
    public List<CartRedis> findAll(Long memberId) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key(memberId));
        return entries.values().stream().map(o -> (CartRedis) o).collect(Collectors.toList());
    }

    /** 특정 아이템 단건 조회 */
    public CartRedis findById(Long memberId, UUID cartId) {
        Object data = redisTemplate.opsForHash().get(key(memberId), cartId.toString());
        return data != null ? (CartRedis) data : null;
    }

    /** 특정 아이템 삭제 */
    public void delete(Long memberId, UUID cartId) {
        redisTemplate.opsForHash().delete(key(memberId), cartId.toString());
    }
}
