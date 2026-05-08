package com.naijavehicle.api.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    @Value("${jwt.refreshExpiration}")
    private long jwtRefreshExpirationMs;

    public String generateToken(String username, int tokenVersion) {
        return buildToken(username, jwtExpirationMs, "access", tokenVersion);
    }

    public String generateRefreshToken(String username) {
        return buildToken(username, jwtRefreshExpirationMs, "refresh", 0);
    }

    private String buildToken(String username, long expirationMs, String type, int tokenVersion) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiryDate)
                .claim("type", type)
                .claim("tokenVersion", tokenVersion)
                .signWith(getSigningKey())
                .compact();
    }

    public String getUsernameFromToken(String token) {
        try {
            return getClaims(token).getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Error extracting username from token", e);
            return null;
        }
    }

    public String getTokenType(String token) {
        try {
            return getClaims(token).get("type", String.class);
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Error extracting token type", e);
            return null;
        }
    }

    public int getTokenVersion(String token) {
        try {
            Integer version = getClaims(token).get("tokenVersion", Integer.class);
            return version != null ? version : 0;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Error extracting token version", e);
            return -1;
        }
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("Expired JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
