package restaurant.site.util;

// Keeps the paging parameters of the list endpoints inside safe limits.
// (Also keeps the Redis cache small: a client cannot create a new cache key
// for every possible size value, because every size above MAX_SIZE becomes MAX_SIZE.)
public final class Paging {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    private Paging() {
    }

    public static int page(int page) {
        return Math.max(page, 0);
    }

    public static int size(int size) {
        return Math.min(Math.max(size, 1), MAX_SIZE);
    }
}
