package restaurant.site.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

// The cache only makes things faster: the database is the real source of data. So when Redis is
// down or slow, a cache error must NOT fail the request. We log it and carry on, and
// the service method simply runs against the database (a miss).
//
// Trade-off: if a cache eviction fails, Redis can still hold an old value until its 10 minute
// expiry (see CacheConfig). That is the price of keeping the site up while Redis is away.
@Slf4j
public class LenientCacheErrorHandler implements CacheErrorHandler {

    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        log.warn("Cache '{}' unavailable, reading from the database instead (key {}): {}",
                cache.getName(), key, exception.getMessage());
    }

    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        log.warn("Cache '{}' unavailable, result not cached (key {}): {}",
                cache.getName(), key, exception.getMessage());
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        log.warn("Cache '{}' unavailable, entry not evicted (key {}): {}",
                cache.getName(), key, exception.getMessage());
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        log.warn("Cache '{}' unavailable, cache not cleared: {}", cache.getName(), exception.getMessage());
    }
}
