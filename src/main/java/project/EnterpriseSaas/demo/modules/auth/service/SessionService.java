// package project.EnterpriseSaas.demo.modules.auth.service;

// import com.fasterxml.jackson.databind.ObjectMapper;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.data.redis.core.StringRedisTemplate;
// import org.springframework.security.crypto.password.PasswordEncoder;
// import org.springframework.stereotype.Service;
// import project.EnterpriseSaas.demo.modules.auth.dto.SessionData;

// import java.time.Instant;
// import java.util.*;
// import java.util.concurrent.ConcurrentHashMap;
// import java.util.concurrent.TimeUnit;

// @Service
// @RequiredArgsConstructor
// @Slf4j
// public class SessionService {

//     @Autowired(required = false)
//     private StringRedisTemplate redisTemplate; // Auto-wired if present, null if not

//     private final PasswordEncoder passwordEncoder;
//     private final ObjectMapper objectMapper;

//     private static final long SESSION_TTL_SECONDS = 7 * 24 * 60 * 60; // 7 days

//     // In-memory fallback cache when Redis is unavailable
//     private final Map<String, MemoryItem> memoryStore = new ConcurrentHashMap<>();

//     private record MemoryItem(SessionData data, long expiresAt) {}

//     // ── Create a new session on login ────────────────────────────────────────

//     public String createSession(UUID userId, String refreshToken, String ip, String userAgent) {
//         String sessionId = UUID.randomUUID().toString();
//         String tokenHash = passwordEncoder.encode(refreshToken);

//         SessionData meta = SessionData.builder()
//                 .tokenHash(tokenHash)
//                 .ip(ip != null ? ip : "unknown")
//                 .userAgent(userAgent != null ? userAgent : "unknown")
//                 .createdAt(Instant.now().toString())
//                 .build();

//         String key = buildKey(userId, sessionId);

//         if (isRedisAvailable()) {
//             try {
//                 String json = objectMapper.writeValueAsString(meta);
//                 redisTemplate.opsForValue().set(key, json, SESSION_TTL_SECONDS, TimeUnit.SECONDS);
//                 log.debug("Session created (Redis): {} for user {}", sessionId, userId);
//                 return sessionId;
//             } catch (Exception e) {
//                 log.warn("Redis write failed, falling back to memory: {}", e.getMessage());
//             }
//         }

//         memoryStore.put(key, new MemoryItem(meta, System.currentTimeMillis() + SESSION_TTL_SECONDS * 1000));
//         log.debug("Session created (Memory): {} for user {}", sessionId, userId);
//         return sessionId;
//     }

//     // ── Check session exists ──────────────────────────────────────────────────

//     public boolean sessionExists(UUID userId, String sessionId) {
//         String key = buildKey(userId, sessionId);

//         if (isRedisAvailable()) {
//             try {
//                 Boolean exists = redisTemplate.hasKey(key);
//                 return Boolean.TRUE.equals(exists);
//             } catch (Exception e) {
//                 return true; // Fail open if Redis drops
//             }
//         }

//         MemoryItem item = memoryStore.get(key);
//         if (item == null) return false;
//         if (System.currentTimeMillis() > item.expiresAt()) {
//             memoryStore.remove(key);
//             return false;
//         }
//         return true;
//     }

//     // ── Validate on token refresh ─────────────────────────────────────────────

//     public boolean validateSession(UUID userId, String sessionId, String refreshToken) {
//         String key = buildKey(userId, sessionId);
//         SessionData meta = getSessionData(key);

//         if (meta == null) return false;

//         boolean valid = passwordEncoder.matches(refreshToken, meta.getTokenHash());
//         if (!valid) return false;

//         // Slide the TTL on successful validation
//         if (isRedisAvailable()) {
//             try {
//                 redisTemplate.expire(key, SESSION_TTL_SECONDS, TimeUnit.SECONDS);
//             } catch (Exception ignored) {}
//         }

//         MemoryItem memItem = memoryStore.get(key);
//         if (memItem != null) {
//             memoryStore.put(key, new MemoryItem(meta, System.currentTimeMillis() + SESSION_TTL_SECONDS * 1000));
//         }

//         return true;
//     }

//     // ── Rotate token (called during refresh) ─────────────────────────────────

//     public void rotateSession(UUID userId, String sessionId, String newRefreshToken) {
//         String key = buildKey(userId, sessionId);
//         SessionData meta = getSessionData(key);

//         if (meta == null) {
//             throw new RuntimeException("Session expired or revoked");
//         }

//         meta.setTokenHash(passwordEncoder.encode(newRefreshToken));
//         meta.setLastSeenAt(Instant.now().toString());

//         if (isRedisAvailable()) {
//             try {
//                 String json = objectMapper.writeValueAsString(meta);
//                 redisTemplate.opsForValue().set(key, json, SESSION_TTL_SECONDS, TimeUnit.SECONDS);
//                 return;
//             } catch (Exception ignored) {}
//         }

//         memoryStore.put(key, new MemoryItem(meta, System.currentTimeMillis() + SESSION_TTL_SECONDS * 1000));
//     }

//     // ── Revoke a single session ──────────────────────────────────────────────

//     public void revokeSession(UUID userId, String sessionId) {
//         String key = buildKey(userId, sessionId);

//         if (isRedisAvailable()) {
//             try {
//                 redisTemplate.delete(key);
//             } catch (Exception ignored) {}
//         }

//         memoryStore.remove(key);
//         log.info("Session revoked: {} for user {}", sessionId, userId);
//     }

//     // ── Revoke all sessions ──────────────────────────────────────────────────

//     public int revokeAllSessions(UUID userId) {
//         int count = 0;
//         String prefix = "session:" + userId + ":";

//         if (isRedisAvailable()) {
//             try {
//                 Set<String> keys = redisTemplate.keys(prefix + "*");
//                 if (keys != null && !keys.isEmpty()) {
//                     count += keys.size();
//                     redisTemplate.delete(keys);
//                 }
//             } catch (Exception ignored) {}
//         }

//         for (String key : memoryStore.keySet()) {
//             if (key.startsWith(prefix)) {
//                 memoryStore.remove(key);
//                 count++;
//             }
//         }

//         log.info("Revoked all sessions for user {}", userId);
//         return count;
//     }

//     // ── Helpers ──────────────────────────────────────────────────────────────

//     private SessionData getSessionData(String key) {
//         if (isRedisAvailable()) {
//             try {
//                 String raw = redisTemplate.opsForValue().get(key);
//                 if (raw != null) {
//                     return objectMapper.readValue(raw, SessionData.class);
//                 }
//             } catch (Exception ignored) {}
//         }

//         MemoryItem item = memoryStore.get(key);
//         if (item != null && System.currentTimeMillis() <= item.expiresAt()) {
//             return item.data();
//         }

//         return null;
//     }

//     private boolean isRedisAvailable() {
//         return redisTemplate != null;
//     }

//     private String buildKey(UUID userId, String sessionId) {
//         return "session:" + userId + ":" + sessionId;
//     }
// }

package project.EnterpriseSaas.demo.modules.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import project.EnterpriseSaas.demo.modules.auth.dto.SessionData;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate; // Auto-wired if present, null if not

    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    private static final long SESSION_TTL_SECONDS = 7 * 24 * 60 * 60; // 7 days

    // In-memory fallback cache when Redis is unavailable
    private final Map<String, MemoryItem> memoryStore = new ConcurrentHashMap<>();

    private record MemoryItem(SessionData data, long expiresAt) {}

    // ── Create a new session on login ────────────────────────────────────────

    public String createSession(UUID userId, String refreshToken, String ip, String userAgent) {
        String sessionId = UUID.randomUUID().toString();
        String tokenHash = passwordEncoder.encode(refreshToken);

        SessionData meta = SessionData.builder()
                .tokenHash(tokenHash)
                .ip(ip != null ? ip : "unknown")
                .userAgent(userAgent != null ? userAgent : "unknown")
                .createdAt(Instant.now().toString())
                .build();

        String key = buildKey(userId, sessionId);

        if (isRedisAvailable()) {
            try {
                String json = objectMapper.writeValueAsString(meta);
                redisTemplate.opsForValue().set(key, json, SESSION_TTL_SECONDS, TimeUnit.SECONDS);
                log.debug("Session created (Redis): {} for user {}", sessionId, userId);
                return sessionId;
            } catch (Exception e) {
                log.warn("Redis write failed, falling back to memory: {}", e.getMessage());
            }
        }

        memoryStore.put(key, new MemoryItem(meta, System.currentTimeMillis() + (SESSION_TTL_SECONDS * 1000L)));
        log.debug("Session created (Memory): {} for user {}", sessionId, userId);
        return sessionId;
    }

    // ── Check session exists ──────────────────────────────────────────────────

    public boolean sessionExists(UUID userId, String sessionId) {
        String key = buildKey(userId, sessionId);

        if (isRedisAvailable()) {
            try {
                Boolean exists = redisTemplate.hasKey(key);
                return Boolean.TRUE.equals(exists);
            } catch (Exception e) {
                return true; // Fail open if Redis drops
            }
        }

        MemoryItem item = memoryStore.get(key);
        if (item == null) return false;
        if (System.currentTimeMillis() > item.expiresAt()) {
            memoryStore.remove(key);
            return false;
        }
        return true;
    }

    //  Find session securely during token refresh 
    public String findSessionIdByRefreshToken(UUID userId, String rawRefreshToken) {
        String prefix = "session:" + userId + ":";

        // 1. Search Redis (Primary)
        if (isRedisAvailable()) {
            try {
                Set<String> keys = redisTemplate.keys(prefix + "*");
                if (keys != null) {
                    for (String key : keys) {
                        String raw = redisTemplate.opsForValue().get(key);
                        if (raw != null) {
                            SessionData data = objectMapper.readValue(raw, SessionData.class);
                            if (passwordEncoder.matches(rawRefreshToken, data.getTokenHash())) {
                                return key.substring(prefix.length());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Redis search failed during refresh: {}", e.getMessage());
            }
        }

        // 2. Search Memory (Fallback)
        for (Map.Entry<String, MemoryItem> entry : memoryStore.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                if (passwordEncoder.matches(rawRefreshToken, entry.getValue().data().getTokenHash())) {
                    return entry.getKey().substring(prefix.length());
                }
            }
        }

        return null;
    }

    // ── Validate on token refresh ─────────────────────────────────────────────

    public boolean validateSession(UUID userId, String sessionId, String refreshToken) {
        String key = buildKey(userId, sessionId);
        SessionData meta = getSessionData(key);

        if (meta == null) return false;

        boolean valid = passwordEncoder.matches(refreshToken, meta.getTokenHash());
        if (!valid) return false;

        // Slide the TTL on successful validation
        if (isRedisAvailable()) {
            try {
                redisTemplate.expire(key, SESSION_TTL_SECONDS, TimeUnit.SECONDS);
            } catch (Exception ignored) {}
        }

        MemoryItem memItem = memoryStore.get(key);
        if (memItem != null) {
            memoryStore.put(key, new MemoryItem(meta, System.currentTimeMillis() + (SESSION_TTL_SECONDS * 1000L)));
        }

        return true;
    }

    // ── Rotate token (called during refresh) ─────────────────────────────────

    public void rotateSession(UUID userId, String sessionId, String newRefreshToken) {
        String key = buildKey(userId, sessionId);
        SessionData meta = getSessionData(key);

        if (meta == null) {
            throw new RuntimeException("Session expired or revoked");
        }

        meta.setTokenHash(passwordEncoder.encode(newRefreshToken));
        meta.setLastSeenAt(Instant.now().toString());

        if (isRedisAvailable()) {
            try {
                String json = objectMapper.writeValueAsString(meta);
                redisTemplate.opsForValue().set(key, json, SESSION_TTL_SECONDS, TimeUnit.SECONDS);
                return;
            } catch (Exception ignored) {}
        }

        memoryStore.put(key, new MemoryItem(meta, System.currentTimeMillis() + (SESSION_TTL_SECONDS * 1000L)));
    }

    // ── Revoke a single session ──────────────────────────────────────────────

    public void revokeSession(UUID userId, String sessionId) {
        String key = buildKey(userId, sessionId);

        if (isRedisAvailable()) {
            try {
                redisTemplate.delete(key);
            } catch (Exception ignored) {}
        }

        memoryStore.remove(key);
        log.info("Session revoked: {} for user {}", sessionId, userId);
    }

    // ── Revoke all sessions ──────────────────────────────────────────────────

    public int revokeAllSessions(UUID userId) {
        int count = 0;
        String prefix = "session:" + userId + ":";

        if (isRedisAvailable()) {
            try {
                Set<String> keys = redisTemplate.keys(prefix + "*");
                if (keys != null && !keys.isEmpty()) {
                    count += keys.size();
                    redisTemplate.delete(keys);
                }
            } catch (Exception ignored) {}
        }

        for (String key : memoryStore.keySet()) {
            if (key.startsWith(prefix)) {
                memoryStore.remove(key);
                count++;
            }
        }

        log.info("Revoked all sessions for user {}", userId);
        return count;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private SessionData getSessionData(String key) {
        if (isRedisAvailable()) {
            try {
                String raw = redisTemplate.opsForValue().get(key);
                if (raw != null) {
                    return objectMapper.readValue(raw, SessionData.class);
                }
            } catch (Exception ignored) {}
        }

        MemoryItem item = memoryStore.get(key);
        if (item != null && System.currentTimeMillis() <= item.expiresAt()) {
            return item.data();
        }

        return null;
    }

    private boolean isRedisAvailable() {
        return redisTemplate != null;
    }

    private String buildKey(UUID userId, String sessionId) {
        return "session:" + userId + ":" + sessionId;
    }
}