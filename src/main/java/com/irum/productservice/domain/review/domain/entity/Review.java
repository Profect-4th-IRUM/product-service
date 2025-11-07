package com.irum.productservice.domain.review.domain.entity;

import com.irum.productservice.domain.product.domain.entity.Product;
import com.irum.global.domain.BaseEntity;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.Where;

@Getter
@Entity
@Table(name = "p_review")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE p_review SET deleted_at = NOW() WHERE review_id = ?")
@Where(clause = "deleted_at IS NULL")
@Check(constraints = "rate BETWEEN 1 AND 5")
public class Review extends BaseEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    @Column(name = "review_id", updatable = false, nullable = false)
    private UUID id;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, columnDefinition = "SMALLINT")
    private Short rate;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "order_detail_id", nullable = false, unique = true)
    private UUID orderDetailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Builder
    private Review(String content, Short rate, Long memberId, Product product, UUID orderDetailId) {
        this.content = content;
        this.rate = rate;
        this.memberId = memberId;
        this.product = product;
        this.orderDetailId = orderDetailId;
    }

    public static Review createReview(String content, Short rate, Long memberId, Product product, UUID orderDetailId) {
        return Review.builder()
                .content(content)
                .rate(rate)
                .memberId(memberId)
                .product(product)
                .orderDetailId(orderDetailId)
                .build();
    }

    public void updateReview(String content, Short rate) {
        if (content != null) this.content = content;
        if (rate != null) this.rate = rate;
    }
}
