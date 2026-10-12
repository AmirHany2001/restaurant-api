package restaurant.site.services.security;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

// Brute-force protection for /auth/login: after MAX_FAILURES wrong logins for the same
// (client IP + email) inside WINDOW_MILLIS, further attempts are refused until the window ends.
//
// The key contains the IP on purpose: if it were only the email, anyone could lock a real
// customer out of their account just by typing wrong passwords for it.
//
// State is kept in memory, so it is per application instance and resets on restart.
// With several instances, move this counter to Redis.
@Service
public class LoginAttemptService {

    private static final int MAX_FAILURES = 5;
    private static final long WINDOW_MILLIS = 15 * 60 * 1000L;
    private static final int CLEANUP_THRESHOLD = 10_000;

    private static class Attempt {
        int failures;
        long windowStart;

        Attempt(long windowStart) {
            this.windowStart = windowStart;
        }
    }

    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();

    public boolean isBlocked(String key) {
        Attempt attempt = attempts.get(key);
        if (attempt == null) {
            return false;
        }
        synchronized (attempt) {
            if (expired(attempt)) {
                attempts.remove(key, attempt);
                return false;
            }
            return attempt.failures >= MAX_FAILURES;
        }
    }

    public void recordFailure(String key) {
        if (attempts.size() > CLEANUP_THRESHOLD) {
            attempts.entrySet().removeIf(entry -> expired(entry.getValue())); // keep memory bounded
        }
        long now = System.currentTimeMillis();
        attempts.compute(key, (k, attempt) -> {
            if (attempt == null || expired(attempt)) {
                attempt = new Attempt(now);
            }
            attempt.failures++;
            return attempt;
        });
    }

    public void reset(String key) {
        attempts.remove(key);
    }

    private boolean expired(Attempt attempt) {
        return System.currentTimeMillis() - attempt.windowStart > WINDOW_MILLIS;
    }
}
