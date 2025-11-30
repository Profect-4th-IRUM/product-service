package com.irum.productservice.global.config;

import org.mockito.Mockito;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.filter.AnnotationTypeFilter;

@TestConfiguration
public class FeignAutoMockConfig implements ImportBeanDefinitionRegistrar, ApplicationContextAware {

    private ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }

    @Override
    public void registerBeanDefinitions(
            org.springframework.core.type.AnnotationMetadata importingClassMetadata,
            BeanDefinitionRegistry registry) {

        // 1) FeignClient annotation 검색기
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(FeignClient.class));

        // 2) open-feign-client repo 패키지를 스캔
        String basePackage = "com.irum.openfeign";

        for (BeanDefinition bd : scanner.findCandidateComponents(basePackage)) {
            try {
                Class<?> feignInterface = Class.forName(bd.getBeanClassName());

                // 3) Mockito Mock 생성
                Object mock = Mockito.mock(feignInterface);

                // 4) Spring Bean 등록
                GenericBeanDefinition definition = new GenericBeanDefinition();
                definition.setBeanClass(feignInterface);
                definition.setInstanceSupplier(() -> mock);

                registry.registerBeanDefinition(feignInterface.getSimpleName(), definition);

            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
