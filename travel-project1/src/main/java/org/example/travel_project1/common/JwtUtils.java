package org.example.travel_project1.common;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;


import org.springframework.beans.factory.annotation.Value;

import javax.crypto.SecretKey;
import java.util.Date;


@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;


    /**
     * 生成 JWT token
     * @param userId 用户 ID
     * @return token 字符串
     */
    public String generateToken(Long userId){

        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * 从 token 中解析用户 ID
     * @param token JWT token
     * @return 用户 ID，解析失败返回 null
     */
    public Long getUserIdFromToken(String token){
        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Long.parseLong(claims.getSubject());
        } catch (Exception e) {
            return null;
        }
    }

    public boolean validateToken(String token){
        return getUserIdFromToken(token) != null;
    }
    
}
