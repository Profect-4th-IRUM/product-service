package com.irum.productservice.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.irum.global.advice.exception.CommonException;
import com.irum.openfeign.product.dto.request.ProductInternalRequest;
import com.irum.openfeign.product.dto.request.RollbackStockRequest;
import com.irum.productservice.domain.category.domain.entity.Category;
import com.irum.productservice.domain.category.domain.repository.CategoryRepository;
import com.irum.productservice.domain.deliverypolicy.domain.entity.DeliveryPolicy;
import com.irum.productservice.domain.deliverypolicy.domain.repository.DeliveryPolicyRepository;
import com.irum.productservice.domain.product.Internal.service.ProductInternalService;
import com.irum.productservice.domain.product.domain.entity.Product;
import com.irum.productservice.domain.product.domain.entity.ProductOptionGroup;
import com.irum.productservice.domain.product.domain.entity.ProductOptionValue;
import com.irum.productservice.domain.product.domain.repository.ProductOptionGroupRepository;
import com.irum.productservice.domain.product.domain.repository.ProductOptionValueRepository;
import com.irum.productservice.domain.product.domain.repository.ProductRepository;
import com.irum.productservice.domain.store.domain.entity.Store;
import com.irum.productservice.domain.store.domain.repository.StoreRepository;
import com.irum.productservice.global.exception.errorcode.ProductErrorCode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.AuditorAware;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
public class ProductInternalIntegrationTest {
    @Autowired private ProductInternalService productInternalService;

    @Autowired private StoreRepository storeRepository;
    @Autowired private DeliveryPolicyRepository deliveryPolicyRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductOptionGroupRepository productOptionGroupRepository;
    @Autowired private ProductOptionValueRepository productOptionValueRepository;

    @MockitoBean private AuditorAware<Long> auditorAware;

    private UUID storeId;
    private UUID optionValueId;
    private final int INITIAL_STOCK = 20; // 초기 재고
    private final int USER_COUNT = 100; // 주문을 요청하는 사용자 수
    private final int ROLLBACK_COUNT = 100; // 롤백을 요청하는 클라이언트 수

    @BeforeEach
    void setUp() {
        // 테스트 데이터 미리 세팅

        // Auditing이 동작할 때 반환할 가짜 사용자 ID 설정
        Long testAuditorId = 1L;
        given(auditorAware.getCurrentAuditor()).willReturn(Optional.of(testAuditorId));

        Store store =
                storeRepository.save(
                        Store.createStore(
                                "테스트 상점", "010-1111-2222", "주소", "1234567890", "1234567890", 1L));
        storeId = store.getId();
        DeliveryPolicy policy = DeliveryPolicy.createPolicy(3000, 30000, 5, store);
        deliveryPolicyRepository.save(policy);

        Category rootCategory =
                categoryRepository.save(Category.createRootCategory("루트 카테고리")); // Category 생성자 가정
        Category category1 =
                categoryRepository.save(
                        Category.createSubCategory("부모 카테고리", rootCategory)); // Category 생성자 가정
        Category category2 =
                categoryRepository.save(
                        Category.createSubCategory("자식 카테고리", category1)); // Category 생성자 가정

        Product product =
                productRepository.save(
                        Product.createProduct(store, category2, "테스트 상품", "설명", "상세", 10000, true));

        ProductOptionGroup group =
                productOptionGroupRepository.save(
                        ProductOptionGroup.createOptionGroup(product, "사이즈"));

        ProductOptionValue pov =
                productOptionValueRepository.save(
                        ProductOptionValue.createOptionValue(group, "L", INITIAL_STOCK, 0));
        optionValueId = pov.getId();
    }

    @Test
    @DisplayName("주문 - 순차 주문(1개씩)")
    void updateStock_SequentialTest() throws InterruptedException {
        // given
        AtomicInteger successCount = new AtomicInteger(); // 성공 카운트
        AtomicInteger outOfStockCount = new AtomicInteger(); // 재고 부족 실패 카운트
        AtomicInteger recoverCount = new AtomicInteger(); // 재시도 횟수 초과 카운트

        // 요청
        ProductInternalRequest.OptionValueRequest optionRequest =
                new ProductInternalRequest.OptionValueRequest(optionValueId, 1); // 1개씩 주문
        ProductInternalRequest request =
                new ProductInternalRequest(List.of(optionRequest), storeId);

        // when
        for (int i = 0; i < USER_COUNT; i++) {
            try {
                // 서비스 로직 호출
                productInternalService.updateStock(request);
                successCount.incrementAndGet();
            } catch (CommonException e) {
                // 재고 부족 예외, 재시도 횟수 초과 예외인지 체크
                if (e.getErrorCode() == ProductErrorCode.PRODUCT_OUT_OF_STOCK) {
                    outOfStockCount.incrementAndGet();
                } else if (e.getErrorCode() == ProductErrorCode.PRODUCT_RETRY_LIMIT_EXCEEDED) {
                    recoverCount.incrementAndGet();
                } else {
                    // 그 외 다른 예외
                    System.err.println("예상치 못한 예외: " + e.getMessage());
                }
            } catch (Exception e) {
                // OptimisticLockException이 @Retryable과 @Recover에 의해 처리되지 않고 테스트까지 올라오는지 확인
                System.err.println(
                        "테스트 중 감지된 예외: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            }
        }

        // then
        System.out.println("======================================");
        System.out.println("성공 건수: " + successCount.get());
        System.out.println("재고 부족 건수: " + outOfStockCount.get());
        System.out.println("재시도 횟수 초과 건수: " + recoverCount.get());
        System.out.println("======================================");

        // [검증]
        // 초기 재고 만큼만 성공해야 함
        assertThat(successCount.get()).isEqualTo(INITIAL_STOCK);
        // 나머지 요청(50 - 20 = 30)은 모두 '재고 부족'이어야 함
        assertThat(outOfStockCount.get() + recoverCount.get())
                .isEqualTo(USER_COUNT - INITIAL_STOCK);
        // DB에서 실제 재고를 다시 조회하여 0인지 확인
        ProductOptionValue finalPov =
                productOptionValueRepository
                        .findById(optionValueId)
                        .orElseThrow(() -> new RuntimeException("테스트용 옵션 조회 실패"));

        assertThat(finalPov.getStockQuantity()).isZero();
    }

    @Test
    @DisplayName("주문 - 동시에 주문 - Optimistic Lock 테스트")
    void updateStock_ConcurrencyTest() throws InterruptedException {
        // given
        ExecutorService executorService = Executors.newFixedThreadPool(USER_COUNT); // 스레드 만들기
        CountDownLatch latch = new CountDownLatch(USER_COUNT); // 스레드 종료 확인

        AtomicInteger successCount = new AtomicInteger(); // 성공 카운트
        AtomicInteger outOfStockCount = new AtomicInteger(); // 재고 부족 실패 카운트
        AtomicInteger recoverCount = new AtomicInteger(); // 재시도 횟수 초과 카운트

        // 요청
        ProductInternalRequest.OptionValueRequest optionRequest =
                new ProductInternalRequest.OptionValueRequest(optionValueId, 1); // 1개씩 주문
        ProductInternalRequest request =
                new ProductInternalRequest(List.of(optionRequest), storeId);

        // when
        for (int i = 0; i < USER_COUNT; i++) {
            executorService.submit(
                    () -> {
                        try {
                            // 서비스 로직 호출
                            productInternalService.updateStock(request);
                            successCount.incrementAndGet();
                        } catch (CommonException e) {
                            // 재고 부족 예외인지, 재시도 횟수 초과 예외인지 확인
                            if (e.getErrorCode() == ProductErrorCode.PRODUCT_OUT_OF_STOCK) {
                                outOfStockCount.incrementAndGet();
                            } else if (e.getErrorCode()
                                    == ProductErrorCode.PRODUCT_RETRY_LIMIT_EXCEEDED) {
                                recoverCount.incrementAndGet();
                            } else {
                                // 그 외 다른 예외
                                System.err.println("예상치 못한 예외: " + e.getMessage());
                            }
                        } catch (Exception e) {
                            // OptimisticLockException이 @Retryable이나 @Recover에 의해 처리되지 않고 테스트까지
                            // 올라오는지 확인
                            System.err.println(
                                    "테스트 중 감지된 예외: "
                                            + e.getClass().getSimpleName()
                                            + " - "
                                            + e.getMessage());
                        } finally {
                            latch.countDown();
                        }
                    });
        }

        // then
        // 모든 스레드가 끝날 때까지 최대 5초 대기
        latch.await(5, TimeUnit.SECONDS);
        executorService.shutdown();

        System.out.println("======================================");
        System.out.println("성공 건수: " + successCount.get());
        System.out.println("재고 부족 건수: " + outOfStockCount.get());
        System.out.println("재시도 횟수 초과 건수: " + recoverCount.get());
        System.out.println("======================================");

        // [최종 검증]
        // 초기 재고만큼만 성공해야 함
        assertThat(successCount.get()).isEqualTo(INITIAL_STOCK);
        // 나머지 요청은 모두 재고 부족이거나 재시도 횟수 초과 이어야 함
        assertThat(outOfStockCount.get() + recoverCount.get())
                .isEqualTo(USER_COUNT - INITIAL_STOCK);
        // DB에서 실제 재고를 다시 조회하여 0인지 확인
        ProductOptionValue finalPov =
                productOptionValueRepository
                        .findById(optionValueId)
                        .orElseThrow(() -> new RuntimeException("테스트용 옵션 조회 실패"));

        assertThat(finalPov.getStockQuantity()).isZero();
    }

    @Test
    @DisplayName("롤백 순차 테스트")
    void rollback_SequentialTest() throws InterruptedException {
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger recoverCount = new AtomicInteger();

        RollbackStockRequest.OptionValueRequest optionValueRequest =
                new RollbackStockRequest.OptionValueRequest(optionValueId, 1);
        RollbackStockRequest request = new RollbackStockRequest(List.of(optionValueRequest));

        for (int i = 0; i < ROLLBACK_COUNT; i++) {
            try {
                productInternalService.rollbackStock(request);
                successCount.incrementAndGet();
            } catch (CommonException e) {
                if (e.getErrorCode() == ProductErrorCode.PRODUCT_RETRY_LIMIT_EXCEEDED) {
                    recoverCount.incrementAndGet();
                } else {
                    System.err.println("예상치 못한 예외: " + e.getMessage());
                }
            } catch (Exception e) {
                System.err.println(
                        "테스트 중 감지된 예외: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            }
        }

        // Then: 최종 재고 확인
        ProductOptionValue finalOption =
                productOptionValueRepository
                        .findById(optionValueId)
                        .orElseThrow(
                                () -> new AssertionError("Test setup failed: Option not found"));

        int expectedStock = INITIAL_STOCK + ROLLBACK_COUNT;

        System.out.println("======================================");
        System.out.println("성공 건수: " + successCount.get());
        System.out.println("재시도 횟수 초과 건수: " + recoverCount.get());
        System.out.println("Initial Stock: " + INITIAL_STOCK);
        System.out.println("Final Stock (Actual): " + finalOption.getStockQuantity());
        System.out.println("Final Stock (Expected): " + expectedStock);
        System.out.println("======================================");

        assertThat(expectedStock).isEqualTo(finalOption.getStockQuantity());
    }

    @Test
    @DisplayName("롤백 동시성 테스트")
    void rollback_ConcurrencyTest() throws InterruptedException {
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger recoverCount = new AtomicInteger();

        ExecutorService executorService = Executors.newFixedThreadPool(ROLLBACK_COUNT);
        CountDownLatch latch = new CountDownLatch(ROLLBACK_COUNT);

        RollbackStockRequest.OptionValueRequest optionValueRequest =
                new RollbackStockRequest.OptionValueRequest(optionValueId, 1);
        RollbackStockRequest request = new RollbackStockRequest(List.of(optionValueRequest));

        for (int i = 0; i < ROLLBACK_COUNT; i++) {
            executorService.submit(
                    () -> {
                        try {
                            productInternalService.rollbackStock(request);
                            successCount.incrementAndGet();
                        } catch (CommonException e) {
                            if (e.getErrorCode() == ProductErrorCode.PRODUCT_RETRY_LIMIT_EXCEEDED) {
                                recoverCount.incrementAndGet();
                            } else {
                                System.err.println("예상치 못한 예외: " + e.getMessage());
                            }
                        } catch (Exception e) {
                            System.err.println(
                                    "테스트 중 감지된 예외: "
                                            + e.getClass().getSimpleName()
                                            + " - "
                                            + e.getMessage());
                        } finally {
                            latch.countDown();
                        }
                    });
        }

        latch.await(15, TimeUnit.SECONDS);
        executorService.shutdown();

        // Then: 최종 재고 확인
        ProductOptionValue finalOption =
                productOptionValueRepository
                        .findById(optionValueId)
                        .orElseThrow(
                                () -> new AssertionError("Test setup failed: Option not found"));

        int expectedStock = INITIAL_STOCK + ROLLBACK_COUNT;

        System.out.println("======================================");
        System.out.println("성공 건수: " + successCount.get());
        System.out.println("재시도 횟수 초과 건수: " + recoverCount.get());
        System.out.println("Initial Stock: " + INITIAL_STOCK);
        System.out.println("Final Stock (Actual): " + finalOption.getStockQuantity());
        System.out.println("Final Stock (Expected): " + expectedStock);
        System.out.println("======================================");

        // 무조건 성공해야함
        assertThat(expectedStock).isEqualTo(finalOption.getStockQuantity());
    }
}
