package org.exh.nianhuawechatminiprogrambackend.utils;

import cn.hutool.core.date.DateUtil;
import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import org.exh.nianhuawechatminiprogrambackend.config.JwtConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Date;

/**
 * JWT工具类
 */
@Slf4j
@Component
public class JwtUtil {

    @Autowired
    private JwtConfig jwtConfig;

    /**
     * 生成Token
     *
     * @param userId 用户ID
     * @return Token字符串
     */
    public String generateToken(Long userId) {
        return generateToken(userId, jwtConfig.getExpiration());
    }

    /**
     * 生成Token（自定义过期时间）
     *
     * @param userId     用户ID
     * @param expireTime 过期时间（毫秒）
     * @return Token字符串
     */
    public String generateToken(Long userId, Long expireTime) {
        Date now = new Date();
        Date expireDate = new Date(now.getTime() + expireTime);

        return Jwts.builder()
                .setHeaderParam("type", "JWT")
                .setSubject(userId.toString())
                .setIssuedAt(now)
                .setExpiration(expireDate)
                .signWith(SignatureAlgorithm.HS256, this.getSecretKey())
                .compact();
    }

    /**
     * 生成Refresh Token
     *
     * @param userId 用户ID
     * @return Refresh Token字符串
     */
    public String generateRefreshToken(Long userId) {
        return generateToken(userId, jwtConfig.getRefreshExpiration());
    }

    /**
     * 解析Token，获取用户ID
     *
     * @param token Token字符串
     * @return 用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = this.getTokenBody(token);
        return Long.parseLong(claims.getSubject());
    }

    /**
     * 验证Token是否有效
     *
     * @param token Token字符串
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(this.getSecretKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Token已过期: {}", token);
        } catch (MalformedJwtException e) {
            log.warn("Token格式错误: {}", token);
        } catch (SignatureException e) {
            log.warn("Token签名错误: {}", token);
        } catch (Exception e) {
            log.warn("Token验证失败: {}", token, e);
        }
        return false;
    }

    /**
     * 验证Token是否过期
     *
     * @param token Token字符串
     * @return 是否过期
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = this.getTokenBody(token);
            Date expiration = claims.getExpiration();
            return expiration.before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        } catch (Exception e) {
            log.error("Token解析失败", e);
            return true;
        }
    }

    /**
     * 获取Token剩余过期时间（秒）
     *
     * @param token Token字符串
     * @return 剩余秒数
     */
    public Long getTokenRemainingTime(String token) {
        try {
            Claims claims = this.getTokenBody(token);
            Date expiration = claims.getExpiration();
            return (expiration.getTime() - System.currentTimeMillis()) / 1000;
        } catch (ExpiredJwtException e) {
            return 0L;
        } catch (Exception e) {
            log.error("Token解析失败", e);
            return 0L;
        }
    }

    /**
     * 刷新Token（生成新的Token）
     *
     * @param token 旧的Token
     * @return 新的Token
     */
    public String refreshToken(String token) {
        try {
            Claims claims = this.getTokenBody(token);
            Long userId = Long.parseLong(claims.getSubject());
            return this.generateToken(userId);
        } catch (Exception e) {
            log.error("Token刷新失败", e);
            throw new RuntimeException("Token刷新失败", e);
        }
    }

    private Claims getTokenBody(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(this.getSecretKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            log.warn("Token已过期: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Token解析失败", e);
            throw new RuntimeException("Token解析失败", e);
        }
    }

    private SecretKey getSecretKey() {
        byte[] encodedKey = Base64.getDecoder().decode(jwtConfig.getSecret());
        return new SecretKeySpec(encodedKey, 0, encodedKey.length, "AES");
    }

    public JwtConfig getJwtConfig() {
        return jwtConfig;
    }
}
