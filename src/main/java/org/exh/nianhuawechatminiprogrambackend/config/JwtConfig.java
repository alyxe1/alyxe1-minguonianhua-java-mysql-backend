package org.exh.nianhuawechatminiprogrambackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT配置类
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {
    /**
     * JWT密钥
     */
    private String secret;

    /**
     * Token过期时间（毫秒）
     */
    private Long expiration;

    /**
     * Refresh Token过期时间（毫秒）
     */
    private Long refreshExpiration;
}
