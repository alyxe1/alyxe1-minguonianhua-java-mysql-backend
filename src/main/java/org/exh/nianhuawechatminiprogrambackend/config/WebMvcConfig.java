package org.exh.nianhuawechatminiprogrambackend.config;

import org.exh.nianhuawechatminiprogrambackend.interceptor.JwtInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC配置类
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    /**
     * 跨域配置
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowCredentials(true)
                .maxAge(3600)
                .allowedHeaders("*");
    }

    /**
     * 拦截器配置
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册JWT拦截器，并配置拦截路径
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/v1/**")  // 拦截所有/api/v1开头的请求
                .excludePathPatterns(
                        "/api/v1/auth/wechat-login",  // 排除微信登录接口
                        "/api/v1/auth/mock-login",    // 排除模拟登录接口
                        "/api/v1/auth/refresh"        // 排除刷新token接口
                );
    }
}
