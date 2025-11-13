package com.irum.productservice.domain.deliverypolicy.service;

import com.irum.global.advice.exception.CommonException;
import com.irum.openfeign.member.dto.response.MemberDto;
import com.irum.openfeign.member.enums.Role;
import com.irum.productservice.domain.deliverypolicy.domain.entity.DeliveryPolicy;
import com.irum.productservice.domain.deliverypolicy.domain.repository.DeliveryPolicyRepository;
import com.irum.productservice.domain.deliverypolicy.dto.request.DeliveryPolicyCreateRequest;
import com.irum.productservice.domain.deliverypolicy.dto.request.DeliveryPolicyInfoUpdateRequest;
import com.irum.productservice.domain.store.domain.entity.Store;
import com.irum.productservice.domain.store.domain.repository.StoreRepository;
import com.irum.productservice.global.util.MemberUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;



import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DeliveryPolicyServiceTest {

    @InjectMocks
    private DeliveryPolicyService deliveryPolicyService;

    @Mock
    private DeliveryPolicyRepository deliveryPolicyRepository;
    @Mock
    private StoreRepository storeRepository;
    @Mock
    private MemberUtil memberUtil;

    private MemberDto member;
    private DeliveryPolicy deliveryPolicy;
    private UUID deliveryPolicyId;
    private Store store;
    private UUID storeId;

    @BeforeEach
    void setUp() throws Exception {

        // 공통 Member 설정
        member = new MemberDto(1L, "이삭", "isak@test.com", "010-1111-1111", Role.OWNER);

        // 공통 Store 기본 생성
        storeId = UUID.randomUUID();
        store = Store.createStore(
                "기본상점",
                "010-2222-3333",
                "서울시 송파구",
                "1234567890",
                "0987654321",
                member.memberId()
        );

        Field storeIdField = Store.class.getDeclaredField("id");
        storeIdField.setAccessible(true);
        storeIdField.set(store, storeId);

        // DeliveryPolicy 생성
        deliveryPolicyId = UUID.randomUUID();
        deliveryPolicy = DeliveryPolicy.createPolicy(
                10000,
                2,
                1000,
                store
        );
        // DeliveryPolicy ID 직접 주입
        try {
            Field policyIdField = DeliveryPolicy.class.getDeclaredField("deliveryPolicyId");
            policyIdField.setAccessible(true);
            policyIdField.set(deliveryPolicy, deliveryPolicyId);
        } catch (NoSuchFieldException e) {
            // 엔티티 필드명이 "deliveryPolicyId"가 아닌 경우 "id"로 fallback
            Field policyIdField = DeliveryPolicy.class.getDeclaredField("id");
            policyIdField.setAccessible(true);
            policyIdField.set(deliveryPolicy, deliveryPolicyId);
        }

        // Store <-> DeliveryPolicy 양방향 연관관계 설정
        Field policyField = Store.class.getDeclaredField("deliveryPolicy");
        policyField.setAccessible(true);
        policyField.set(store, deliveryPolicy);

    }

    @DisplayName("배송정책 생성 성공 테스트")
    @Test
    void createDeliveryPolicy_SuccessTest() {
        // given
        when(memberUtil.getCurrentMember()).thenReturn(member);
        when(storeRepository.findByMember(member.memberId())).thenReturn(Optional.of(store));
        when(deliveryPolicyRepository.save(any(DeliveryPolicy.class))).thenReturn(deliveryPolicy);

        DeliveryPolicyCreateRequest request =
                new DeliveryPolicyCreateRequest(10000, 2, 1000);

        // when
        deliveryPolicyService.createDeliveryPolicy(request);

        // then
        verify(deliveryPolicyRepository, times(1)).save(any(DeliveryPolicy.class));
    }

    @DisplayName("배송비 정책 수정 성공 테스트")
    @Test
    void updateDeliveryPolicy_SuccessTest() {
        // given
        UUID policyId = deliveryPolicyId;

        // changeDeliveryPolicy() 내부에서 validDeliveryPolicy() → findById() 호출하므로 Mock 필요
        when(deliveryPolicyRepository.findById(policyId)).thenReturn(Optional.of(deliveryPolicy));

        DeliveryPolicyInfoUpdateRequest request =
                new DeliveryPolicyInfoUpdateRequest(30000, 3, 2000);

        // when
        deliveryPolicyService.changeDeliveryPolicy(policyId, request);

        // then
        assertThat(deliveryPolicy.getDefaultDeliveryFee()).isEqualTo(30000);
        assertThat(deliveryPolicy.getMinQuantity()).isEqualTo(3);
        assertThat(deliveryPolicy.getMinAmount()).isEqualTo(2000);

        verify(deliveryPolicyRepository, times(1)).findById(policyId);
    }

    @DisplayName("배송정책 직접 삭제 성공 테스트")
    @Test
    void withdrawDeliveryPolicy_SuccessTest() {
        // given
        UUID policyId = deliveryPolicyId;
        when(deliveryPolicyRepository.findById(policyId)).thenReturn(Optional.of(deliveryPolicy));
        when(memberUtil.getCurrentMember()).thenReturn(member);

        // when
        deliveryPolicyService.withdrawDeliveryPolicy(policyId);

        // then
        assertThat(deliveryPolicy.getDeletedAt()).isNotNull();
        assertThat(deliveryPolicy.getDeletedBy()).isEqualTo(member.memberId());
        verify(deliveryPolicyRepository, times(1)).findById(policyId);
        verify(memberUtil, times(1)).getCurrentMember();

    }

    @DisplayName("상점 ID로 배송정책 삭제 성공 테스트")
    @Test
    void deleteDeliveryPolicyByStoreId_SuccessTest() {
        // given
        UUID storeId = this.storeId;
        when(deliveryPolicyRepository.findByStoreId(storeId)).thenReturn(Optional.of(deliveryPolicy));

        // when
        deliveryPolicyService.deleteDeliveryPolicyByStoreId(storeId, 99L); // 99L = 관리자 ID

        // then
        assertThat(deliveryPolicy.getDeletedAt()).isNotNull();
        assertThat(deliveryPolicy.getDeletedBy()).isEqualTo(99L);
        verify(deliveryPolicyRepository, times(1)).findByStoreId(storeId);

    }

    @DisplayName("배송정책 직접 삭제 실패 테스트 - 정책 없음")
    @Test
    void withdrawDeliveryPolicy_FailTest_NotFound() {
        // given
        UUID invalidId = UUID.randomUUID();
        when(deliveryPolicyRepository.findById(invalidId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> deliveryPolicyService.withdrawDeliveryPolicy(invalidId))
                .isInstanceOf(CommonException.class)
                .hasMessageContaining("배송비 정책을 찾을 수 없습니다");

        verify(deliveryPolicyRepository, times(1)).findById(invalidId);
    }

    @DisplayName("배송비 정책 조회 성공 테스트")
    @Test
    void findDeliveryPolicy_SuccessTest() {
        // given
        when(deliveryPolicyRepository.findById(deliveryPolicyId)).thenReturn(Optional.of(deliveryPolicy));

        // when
        var response = deliveryPolicyService.findDeliveryPolicy(deliveryPolicyId);

        // then
        assertThat(response).isNotNull();

        verify(deliveryPolicyRepository, times(1)).findById(deliveryPolicyId);
    }
}
