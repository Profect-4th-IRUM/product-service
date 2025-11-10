 package com.irum.productservice.global.config;

 import com.irum.global.infrastructure.config.GlobalAutoConfiguration;
 import com.irum.productservice.domain.cart.service.CartService;
 import com.irum.productservice.domain.category.service.CategoryService;
 import com.irum.productservice.domain.discount.service.DiscountService;
 import com.irum.productservice.domain.product.service.ProductImageService;
 import com.irum.productservice.domain.review.service.ReviewService;

 import org.mockito.Mockito;
 import org.springframework.boot.test.context.TestConfiguration;
 import org.springframework.context.annotation.Bean;
 import org.springframework.context.annotation.Import;

 @TestConfiguration
 @Import(GlobalAutoConfiguration.class)
 public class TestConfig {




    @Bean
    public DiscountService discountService() {
        return Mockito.mock(DiscountService.class);
    }

    @Bean
    public CategoryService categoryService() {
        return Mockito.mock(CategoryService.class);
    }

    @Bean
    public ReviewService reviewService() {
        return Mockito.mock(ReviewService.class);
    }

    public CartService cartService() {
        return Mockito.mock(CartService.class);
    }





    @Bean
    public ProductImageService productImageService() {
        return Mockito.mock(ProductImageService.class);
    }
 }
