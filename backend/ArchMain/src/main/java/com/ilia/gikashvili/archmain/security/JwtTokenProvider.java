package com.ilia.gikashvili.archmain.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * JWT token generation and validation.
 */
@Component
public class JwtTokenProvider {

    private final SecretKey signingKey;
    private final long accessTokenValidityMs;
    private final long refreshTokenValidityMs;

    public JwtTokenProvider(
            @Value("${archmain.jwt.secret}") String secret,
            @Value("${archmain.jwt.access-token-validity-ms:3600000}") long accessTokenValidityMs,
            @Value("${archmain.jwt.refresh-token-validity-ms:604800000}") long refreshTokenValidityMs) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidityMs = accessTokenValidityMs;
        this.refreshTokenValidityMs = refreshTokenValidityMs;
    }

    /**
     * Generate an access token for the given user.
     */
    public String generateAccessToken(UUID userId, String email, UUID sid) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("type", "access").claim("sid", sid.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(accessTokenValidityMs)))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Generate a refresh token for the given user.
     */
    public String generateRefreshToken(UUID userId, UUID sid, String refreshId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("type", "refresh").claim("sid", sid.toString()).id(refreshId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(refreshTokenValidityMs)))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Parse and validate a JWT token. Returns claims or throws on invalid/expired tokens.
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extract user ID from claims.
     */
    public UUID getUserId(Claims claims) {
        return UUID.fromString(claims.getSubject());
    }

    /**
     * Extract email from claims.
     */
    public String getEmail(Claims claims) {
        return claims.get("email", String.class);
    }

    /**
     * Check if the token is an access token.
     */
    public boolean isAccessToken(Claims claims) {
        return "access".equals(claims.get("type", String.class));
    }

    /**
     * Generate a password reset token (valid for 15 minutes).
     */
    public String generatePasswordResetToken(UUID userId, String email) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("type", "password_reset").id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(900))) // 15 mins
                .signWith(signingKey)
                .compact();
    }

    /**
     * Generate an email verification token (valid for 24 hours).
     */
    public String generateEmailVerificationToken(UUID userId, String email) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("type", "email_verification")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(86400))) // 24 hours
                .signWith(signingKey)
                .compact();
    }

    public boolean isPasswordResetToken(Claims claims) {
        return "password_reset".equals(claims.get("type", String.class));
    }

    public boolean isEmailVerificationToken(Claims claims) {
        return "email_verification".equals(claims.get("type", String.class));
    }
}
