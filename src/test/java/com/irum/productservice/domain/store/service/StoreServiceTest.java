package com.irum.productservice.domain.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.irum.openfeign.member.dto.response.MemberDto;
import com.irum.openfeign.member.enums.Role;
import com.irum.productservice.domain.product.domain.repository.ProductRepository;
import com.irum.productservice.domain.product.dto.response.ProductCursorResponse;
import com.irum.productservice.domain.product.dto.response.ProductResponse;
import com.irum.productservice.domain.store.domain.entity.Store;
import com.irum.productservice.domain.store.domain.repository.StoreRepository;
import com.irum.productservice.domain.store.dto.request.StoreCreateRequest;
import com.irum.productservice.domain.store.dto.request.StoreUpdateRequest;
import com.irum.productservice.domain.store.dto.response.StoreCreateResponse;
import com.irum.productservice.global.util.MemberUtil;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class StoreServiceTest {

    @InjectMocks private StoreService storeService;
    @Mock private StoreRepository storeRepository;
    @Mock private MemberUtil memberUtil;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private ProductRepository productRepository;

    private MemberDto member;
    private Store store;
    private UUID storeId;

    // 공통 준비
    @BeforeEach
    void setup() throws Exception {
        // 공통 Member 설정
        member = new MemberDto(1L, "이삭", "isak@test.com", "010-1111-1111", Role.OWNER);

        // 공통 Store 기본 생성
        storeId = UUID.randomUUID();
        store =
                Store.createStore(
                        "기본상점",
                        "010-2222-3333",
                        "서울시 송파구",
                        "1234567890",
                        "0987654321",
                        member.memberId());
        Field idField = Store.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(store, storeId);
    }

    @DisplayName("상점 생성 성공 테스트")
    @Test
    void createStore_SuccessTest() {
        when(memberUtil.getCurrentMember()).thenReturn(member);
        when(storeRepository.existsByMember(member.memberId())).thenReturn(false);
        when(storeRepository.existsByBusinessRegistrationNumber(anyString())).thenReturn(false);
        when(storeRepository.existsByTelemarketingRegistrationNumber(anyString()))
                .thenReturn(false);
        // given
        StoreCreateRequest request =
                new StoreCreateRequest(
                        "테스트상점", "010-1234-5678", "서울시 강남구", "1234567890", "0987654321");

        // save() 시 ID 자동 주입
        when(storeRepository.save(any(Store.class)))
                .thenAnswer(
                        (Answer<Store>)
                                invocation -> {
                                    Store s = invocation.getArgument(0);
                                    Field field = Store.class.getDeclaredField("id");
                                    field.setAccessible(true);
                                    field.set(s, UUID.randomUUID());
                                    return s;
                                });
        // when
        StoreCreateResponse response = storeService.createStore(request);

        // then
        System.out.println("상점 생성 아이디: " + response.storeId());
        assertThat(response).isNotNull();
        assertThat(response.storeId()).isNotNull();
        verify(storeRepository, times(1)).save(any(Store.class));
    }

    @DisplayName("상점 정보 수정 성공 테스트")
    @Test
    void updateStore_SuccessTest() {
        // given
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));

        StoreUpdateRequest updateRequest =
                new StoreUpdateRequest("수정상점", "010-9999-8888", "서울시 강남구");

        // when
        storeService.changeStore(storeId, updateRequest);

        // then
        assertThat(store.getName()).isEqualTo("수정상점");
        assertThat(store.getContact()).isEqualTo("010-9999-8888");
        assertThat(store.getAddress()).isEqualTo("서울시 강남구");
        verify(memberUtil, times(1)).assertMemberResourceAccess(anyLong());
    }

    @DisplayName("상점 삭제 성공 테스트")
    @Test
    void deleteStore_SuccessTest() {
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(memberUtil.getCurrentMember()).thenReturn(member);
        // when
        storeService.withdrawStore(storeId);

        // then
        System.out.println("상점 삭제 시간: " + store.getDeletedAt());
        verify(memberUtil, times(1)).assertMemberResourceAccess(anyLong());
    }

    @DisplayName("상점 조회 성공 테스트")
    @Test
    void findStoreInfo_SuccessTest() {
        // given
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        //
        var response = storeService.findStoreInfo(storeId);

        // then
        System.out.println("조회된 상점명: " + response.name());
        assertThat(response).isNotNull();
        verify(storeRepository, times(1)).findById(storeId);
    }

    @DisplayName("내 상점 상품 조회 성공 테스트")
    @Test
    void findMyStoreProducts_SuccessTest() {
        // given
        UUID cursor = UUID.randomUUID();
        when(storeRepository.findByMember(member.memberId())).thenReturn(Optional.of(store));
        when(memberUtil.getCurrentMember()).thenReturn(member);

        // 상품 Mock
        List<ProductResponse> products =
                List.of(
                        new ProductResponse(
                                storeId, // id
                                "상품1", // name
                                "설명1", // description
                                "상세설명1", // detailDescription
                                1000, // price
                                true, // isPublic
                                4.5, // avgRating
                                10, // reviewCount
                                UUID.randomUUID(), // categoryId
                                "식품" // categoryName
                                ),
                        new ProductResponse(
                                storeId,
                                "상품2",
                                "설명2",
                                "상세설명2",
                                2000,
                                true,
                                4.8,
                                5,
                                UUID.randomUUID(),
                                "생활용품"));
        when(productRepository.findProductsByStoreWithCursor(eq(storeId), eq(cursor), eq(10)))
                .thenReturn(products);

        // when
        ProductCursorResponse response = storeService.getMyStoreProducts(cursor, null);

        // then
        System.out.println("조회된 상품 수: " + response.products().size());
        assertThat(response).isNotNull();
        assertThat(response.products()).hasSize(2);
        verify(storeRepository, times(1)).findByMember(member.memberId());
        verify(productRepository, times(1))
                .findProductsByStoreWithCursor(eq(storeId), eq(cursor), eq(10));
    }
}
