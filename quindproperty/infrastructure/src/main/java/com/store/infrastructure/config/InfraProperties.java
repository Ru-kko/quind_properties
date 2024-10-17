package com.store.infrastructure.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
@ConfigurationProperties(prefix = "app")
public class InfraProperties {
    @Value("${app.infra.page-size}")
    private Integer pageSize;
    @Value("${app.infra.property.time2delete:2592000000}")
    private Long time2DeleteProperty; // Default 30 days
}
