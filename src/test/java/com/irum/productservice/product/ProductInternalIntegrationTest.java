package com.irum.productservice.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.irum.global.advice.exception.CommonException;
import com.irum.openfeign.product.dto.request.RollbackStockRequest;
import com.irum.openfeign.product.dto.request.UpdateStockRequest;
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
import java.util.concurrent.ThreadLocalRandom;
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

    private Store store;
    private DeliveryPolicy policy;
    private Category rootCategory;
    private Category category1;
    private Category category2;
    private Product product;
    private ProductOptionGroup optionGroup;
    private ProductOptionValue optionValue;

    private final int INITIAL_STOCK = 20; // 초기 재고
    private final int USER_COUNT = 100; // 주문 요청 사용자 수
    private final int ROLLBACK_COUNT = 100; // 롤백 요청 수

    // 유니크 제약 피하려고 테스트용 member 값 랜덤 생성
    private long randomMemberId() {
        return ThreadLocalRandom.current().nextLong(1L, 1_000_000_000L);
    }

    private String random10Digits() {
        return String.format("%010d", ThreadLocalRandom.current().nextInt(0, 1_000_000_000));
    }

    @BeforeEach
    void setUp() {
        Long testAuditorId = 1L;
        given(auditorAware.getCurrentAuditor()).willReturn(Optional.of(testAuditorId));

        String businessNo = random10Digits();
        String telemarketingNo = random10Digits();
        long memberId = randomMemberId();

        // Store
        store =
                storeRepository.save(
                        Store.createStore(
                                "테스트 상점",
                                "010-1111-2222",
                                "주소",
                                businessNo,
                                telemarketingNo,
                                memberId));
        storeId = store.getId();

        // DeliveryPolicy
        policy = DeliveryPolicy.createPolicy(3000, 30000, 5, store);
        policy = deliveryPolicyRepository.save(policy);

        // Category 계층
        rootCategory = categoryRepository.save(Category.createRootCategory("루트 카테고리"));
        category1 = categoryRepository.save(Category.createSubCategory("부모 카테고리", rootCategory));
        category2 = categoryRepository.save(Category.createSubCategory("자식 카테고리", category1));

        // Product
        product =
                productRepository.save(
                        Product.createProduct(store, category2, "테스트 상품", "설명", "상세", 10000, true));

        // OptionGroup
        optionGroup =
                productOptionGroupRepository.save(
                        ProductOptionGroup.createOptionGroup(product, "사이즈"));

        // OptionValue
        optionValue =
                productOptionValueRepository.save(
                        ProductOptionValue.createOptionValue(optionGroup, "L", INITIAL_STOCK, 0));
        optionValueId = optionValue.getId();
    }

    @Test
    @DisplayName("주문 - 순차 주문(1개씩)")
    void updateStock_SequentialTest() {
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger outOfStockCount = new AtomicInteger();
        AtomicInteger recoverCount = new AtomicInteger();

        UpdateStockRequest.OptionValueRequest optionRequest =
                new UpdateStockRequest.OptionValueRequest(optionValueId, 1);
        UpdateStockRequest request = new UpdateStockRequest(List.of(optionRequest), storeId);

        for (int i = 0; i < USER_COUNT; i++) {
            try {
                productInternalService.updateStock(request);
                successCount.incrementAndGet();
            } catch (CommonException e) {
                if (e.getErrorCode() == ProductErrorCode.PRODUCT_OUT_OF_STOCK) {
                    outOfStockCount.incrementAndGet();
                } else if (e.getErrorCode() == ProductErrorCode.PRODUCT_RETRY_LIMIT_EXCEEDED) {
                    recoverCount.incrementAndGet();
                } else {
                    System.err.println("예상치 못한 예외: " + e.getMessage());
                }
            } catch (Exception e) {
                System.err.println(
                        "테스트 중 감지된 예외: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            }
        }

        System.out.println("======================================");
        System.out.println("성공 건수: " + successCount.get());
        System.out.println("재고 부족 건수: " + outOfStockCount.get());
        System.out.println("재시도 횟수 초과 건수: " + recoverCount.get());
        System.out.println("======================================");

        assertThat(successCount.get()).isEqualTo(INITIAL_STOCK);
        assertThat(outOfStockCount.get() + recoverCount.get())
                .isEqualTo(USER_COUNT - INITIAL_STOCK);

        ProductOptionValue finalPov =
                productOptionValueRepository
                        .findById(optionValueId)
                        .orElseThrow(() -> new RuntimeException("테스트용 옵션 조회 실패"));

        assertThat(finalPov.getStockQuantity()).isZero();
    }

    @Test
    @DisplayName("주문 - 동시에 주문 - Optimistic Lock 테스트")
    void updateStock_ConcurrencyTest() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(USER_COUNT);
        CountDownLatch latch = new CountDownLatch(USER_COUNT);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger outOfStockCount = new AtomicInteger();
        AtomicInteger recoverCount = new AtomicInteger();

        UpdateStockRequest.OptionValueRequest optionRequest =
                new UpdateStockRequest.OptionValueRequest(optionValueId, 1);
        UpdateStockRequest request = new UpdateStockRequest(List.of(optionRequest), storeId);

        for (int i = 0; i < USER_COUNT; i++) {
            executorService.submit(
                    () -> {
                        try {
                            productInternalService.updateStock(request);
                            successCount.incrementAndGet();
                        } catch (CommonException e) {
                            if (e.getErrorCode() == ProductErrorCode.PRODUCT_OUT_OF_STOCK) {
                                outOfStockCount.incrementAndGet();
                            } else if (e.getErrorCode()
                                    == ProductErrorCode.PRODUCT_RETRY_LIMIT_EXCEEDED) {
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

        latch.await(5, TimeUnit.SECONDS);
        executorService.shutdown();

        System.out.println("======================================");
        System.out.println("성공 건수: " + successCount.get());
        System.out.println("재고 부족 건수: " + outOfStockCount.get());
        System.out.println("재시도 횟수 초과 건수: " + recoverCount.get());
        System.out.println("======================================");

        assertThat(successCount.get()).isEqualTo(INITIAL_STOCK);
        assertThat(outOfStockCount.get() + recoverCount.get())
                .isEqualTo(USER_COUNT - INITIAL_STOCK);

        ProductOptionValue finalPov =
                productOptionValueRepository
                        .findById(optionValueId)
                        .orElseThrow(() -> new RuntimeException("테스트용 옵션 조회 실패"));

        assertThat(finalPov.getStockQuantity()).isZero();
    }

    @Test
    @DisplayName("롤백 순차 테스트")
    void rollback_SequentialTest() {
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
}
