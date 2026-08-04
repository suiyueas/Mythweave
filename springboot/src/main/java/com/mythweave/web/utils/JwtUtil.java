package com.mythweave.web.utils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT工具类
 * 
 * 用于生成和验证JSON Web Token (JWT)
 * JWT用于用户身份认证，token中包含用户ID和用户名信息
 * 
 * token结构：
 * - subject: 用户ID
 * - claim: username 用户名
 * - iat: 签发时间
 * - exp: 过期时间
 */
@Component
public class JwtUtil {
    
    /** JWT签名密钥（从配置文件读取） */
    @Value("${app.jwt.secret}")
    private String secret;

    /** token过期时间（毫秒，从配置文件读取） */
    @Value("${app.jwt.expiration}")
    private Long expiration;

    /**
     * 生成JWT token
     * 
     * @param userId 用户ID
     * @param username 用户名
     * @return JWT token字符串
     */
    public String generateToken(Long userId, String username) {
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("username", username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 从token中解析用户ID
     * 
     * @param token JWT token字符串
     * @return 用户ID
     */
    public Long getUserIdFromToken(String token) {
        return Long.parseLong(Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject());
    }

    /**
     * 验证token是否有效
     * 
     * 验证内容包括：
     * - token格式是否正确
     * - 签名是否匹配
     * - 是否在有效期内
     * 
     * @param token JWT token字符串
     * @return 有效返回true，无效返回false
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}