package com.irum.productservice;

import com.irum.productservice.global.config.FeignAutoMockConfig;
import com.irum.productservice.global.config.TestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import({TestConfig.class, FeignAutoMockConfig.class})
class ProductServiceApplicationTests {

    @Test
    void contextLoads() {}
}
