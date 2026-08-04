package com.mythweave.web.config;

import com.mythweave.web.filter.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置类
 * 
 * 主要配置内容：
 * - CORS跨域资源共享配置
 * - JWT认证过滤器注册
 * 
 * 允许所有来源的API请求进行跨域访问
 * JWT过滤器对所有/api/*路径进行认证拦截
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final JwtFilter jwtFilter;

    /**
     * 配置CORS跨域映射
     * 
     * 允许以下配置：
     * - 路径：/api/**（所有API接口）
     * - 方法：GET, POST, PUT, DELETE, OPTIONS
     * - 请求头：所有请求头
     * - 凭证：支持携带cookies
     * - 来源：所有来源（使用通配符模式）
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    /**
     * 注册JWT认证过滤器
     * 
     * 配置过滤器：
     * - 拦截路径：/api/*
     * - 执行顺序：1（优先执行）
     * 
     * 过滤器会验证请求中的JWT token
     * 验证通过后将用户ID设置到request属性中
     * 验证失败返回401未授权错误
     */
    @Bean
    public FilterRegistrationBean<JwtFilter> jwtFilterRegistration() {
        FilterRegistrationBean<JwtFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(jwtFilter);
        registration.addUrlPatterns("/api/*");
        registration.setOrder(1);
        return registration;
    }
}