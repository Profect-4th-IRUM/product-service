package com.irum.productservice.domain.cart.domain.entity;

import com.irum.global.domain.BaseEntity;
import com.irum.productservice.domain.product.domain.entity.ProductOptionValue;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.Where;

@Entity
@Getter
@Table(name = "p_cart")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE p_cart SET deleted_at = NOW() WHERE cart_id = ?")
@Where(clause = "deleted_at IS NULL")
public class Cart extends BaseEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    @Column(name = "cart_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_value_id", nullable = false)
    private ProductOptionValue optionValue;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Builder(access = AccessLevel.PRIVATE)
    private Cart(Long memberId, ProductOptionValue optionValue, int quantity) {
        this.memberId = memberId;
        this.optionValue = optionValue;
        this.quantity = quantity;
    }

    public static Cart createCart(Long memberId, ProductOptionValue optionValue, int quantity) {
        return Cart.builder()
                .memberId(memberId)
                .optionValue(optionValue)
                .quantity(quantity)
                .build();
    }

    public void updateQuantity(int quantity) {
        this.quantity = quantity;
    }
}
