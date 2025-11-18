package com.irum.productservice.domain.cart.domain.repository;

import com.irum.productservice.domain.cart.domain.entity.CartItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;

public interface CartItemRepository extends CrudRepository<CartItem, String> {

    List<CartItem> findByMemberId(Long memberId);

    Optional<CartItem> findByMemberIdAndCartItemId(Long memberId, String cartItemId);

    Optional<CartItem> findByMemberIdAndOptionValueId(Long memberId, UUID optionValueId);
}
