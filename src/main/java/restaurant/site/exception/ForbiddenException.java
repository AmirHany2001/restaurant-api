package restaurant.site.exception;


// 403 — access not allowed
// Usage: user accessing another user's vehicle, non-admin hitting admin endpoint

import lombok.Getter;

@Getter
public class ForbiddenException extends RuntimeException {

    private final String code;
    private final Object[] args;

    public ForbiddenException(String code) {
        super(code);
        this.code = code;
        this.args = null;
    }

    public ForbiddenException(String code, Object... args) {
        super(code);
        this.code = code;
        this.args = args;
    }
}
