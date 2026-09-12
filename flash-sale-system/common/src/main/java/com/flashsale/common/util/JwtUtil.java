package com.flashsale.common.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JwtUtil {
    /** 密钥支持环境变量 JWT_SECRET 覆盖（生产环境务必设置），默认值仅用于本地开发 */
    private static final SecretKey KEY = Keys.hmacShaKeyFor(
            System.getenv().getOrDefault("JWT_SECRET", "FlashSaleSystemSecretKey2026VeryLongAndSecure")
                    .getBytes(StandardCharsets.UTF_8));
    private static final long EXPIRE = 7 * 24 * 60 * 60 * 1000L; // 7天

    public static String generate(Long userId, String username, String role) {
        return Jwts.builder()
                .claim("userId", userId)
                .claim("username", username)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRE))
                .signWith(KEY)
                .compact();
    }

    public static Claims parse(String token) {
        return Jwts.parser().verifyWith(KEY).build()
                .parseSignedClaims(token).getPayload();
    }

    public static Long getUserId(String token) {
        return parse(token).get("userId", Long.class);
    }

    public static String getRole(String token) {
        return parse(token).get("role", String.class);
    }

    public static boolean validate(String token) {
        try { parse(token); return true; }
        catch (JwtException | IllegalArgumentException e) { return false; }
    }
}