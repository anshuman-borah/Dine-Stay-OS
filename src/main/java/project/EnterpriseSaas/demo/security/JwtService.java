package project.EnterpriseSaas.demo.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${app.jwt.secret:super-secret-jwt-key-change-in-production-super-secret}")
    private String accessSecret;

    @Value("${app.jwt.expiration-ms:900000}") // 15 minutes
    private long accessExpirationMs;

    @Value("${app.jwt.refresh-secret:super-secret-refresh-key-change-in-production-super-secret}")
    private String refreshSecret;

    @Value("${app.jwt.refresh-expiration-ms:604800000}") // 7 days
    private long refreshExpirationMs;

    // ── Token Generation ─────────────────────────────────────────────────────

    public String generateAccessToken(
            String userId,
            String email,
            String tenantId,
            String branchId,
            String role,
            String sessionId
    ) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", userId);
        claims.put("email", email);
        claims.put("tenantId", tenantId);
        if (branchId != null) claims.put("branchId", branchId);
        claims.put("role", role);
        if (sessionId != null) claims.put("sessionId", sessionId);

        return buildToken(claims, userId, accessExpirationMs, getAccessSigningKey());
    }

    public String generateRefreshToken(String userId, String email) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", email);
        return buildToken(claims, userId, refreshExpirationMs, getRefreshSigningKey());
    }

    // ── Claim Extraction ─────────────────────────────────────────────────────

    public String extractUserId(String token) {
        return extractClaim(token, Claims::getSubject, getAccessSigningKey());
    }

    public String extractEmail(String token) {
        return extractClaim(token, claims -> claims.get("email", String.class), getAccessSigningKey());
    }

    public String extractTenantId(String token) {
        return extractClaim(token, claims -> claims.get("tenantId", String.class), getAccessSigningKey());
    }

    public String extractBranchId(String token) {
        return extractClaim(token, claims -> claims.get("branchId", String.class), getAccessSigningKey());
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class), getAccessSigningKey());
    }

    public String extractSessionId(String token) {
        return extractClaim(token, claims -> claims.get("sessionId", String.class), getAccessSigningKey());
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver, SecretKey key) {
        final Claims claims = extractAllClaims(token, key);
        return claimsResolver.apply(claims);
    }

    // ── Token Validation ─────────────────────────────────────────────────────

    public boolean isAccessTokenValid(String token) {
        try {
            return !isTokenExpired(token, getAccessSigningKey());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean isRefreshTokenValid(String token) {
        try {
            return !isTokenExpired(token, getRefreshSigningKey());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // ── Internals ────────────────────────────────────────────────────────────

    private String buildToken(Map<String, Object> claims, String subject, long expirationMs, SecretKey key) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key)
                .compact();
    }

    private boolean isTokenExpired(String token, SecretKey key) {
        return extractClaim(token, Claims::getExpiration, key).before(new Date());
    }

    private Claims extractAllClaims(String token, SecretKey key) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getAccessSigningKey() {
        byte[] keyBytes = accessSecret.getBytes(StandardCharsets.UTF_8);
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    private SecretKey getRefreshSigningKey() {
        byte[] keyBytes = refreshSecret.getBytes(StandardCharsets.UTF_8);
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }
}