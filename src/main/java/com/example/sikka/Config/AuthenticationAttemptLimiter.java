package com.example.sikka.Config;

import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Conservative per-process throttle. These keys must not contain secrets.
 * Limits are also enforced at the web entry point for login and code verification.
 */
@Component
public class AuthenticationAttemptLimiter {
    private static final int LIMIT = 5;
    private static final long WINDOW_SECONDS = 600;
    private record State(long expiresAt, int failures) {}
    private final ConcurrentHashMap<String, State> attempts = new ConcurrentHashMap<>();

    public boolean allowed(String purpose, String identifier) {
        String key = key(purpose, identifier);
        State state = attempts.get(key);
        if (state == null) return true;
        if (state.expiresAt() <= Instant.now().getEpochSecond()) {
            attempts.remove(key, state);
            return true;
        }
        return state.failures() < LIMIT;
    }

    public void failed(String purpose, String identifier) {
        long now = Instant.now().getEpochSecond();
        attempts.compute(key(purpose, identifier), (key, state) ->
                state == null || state.expiresAt() <= now
                        ? new State(now + WINDOW_SECONDS, 1)
                        : new State(state.expiresAt(), Math.min(LIMIT, state.failures() + 1)));
        // Bound memory even if attackers spray unique identities.
        if (attempts.size() > 10000) {
            attempts.entrySet().removeIf(entry -> entry.getValue().expiresAt() <= now);
            // Limit a hostile spray of never-before-seen identifiers.
            if (attempts.size() > 10000) {
                attempts.keySet().stream().limit(1000).toList().forEach(attempts::remove);
            }
        }
    }

    public void succeeded(String purpose, String identifier) {
        attempts.remove(key(purpose, identifier));
    }

    private String key(String purpose, String identifier) {
        String normalized = identifier == null ? "" : identifier.trim().toLowerCase(java.util.Locale.ROOT);
        // Hash identities so raw account emails are not retained in the rate-limit map.
        try {
            byte[] hash = java.security.MessageDigest.getInstance("SHA-256")
                    .digest((purpose + ":" + normalized).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
