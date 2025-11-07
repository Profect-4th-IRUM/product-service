package com.irum.productservice.global.infrastructure.properties;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({RedisProperties.class, FileProperties.class})
public class PropertiesConfig {}
