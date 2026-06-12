package com.ackenieo.init_pro.infrastructure.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * DDD架构配置类
 * 定义各层的组件扫描路径
 */
@Configuration
@ComponentScan(basePackages = {
    "com.ackenieo.init_pro.application",
    "com.ackenieo.init_pro.infrastructure",
    "com.ackenieo.init_pro.interfaces"
})
public class DddArchitectureConfig {
}
