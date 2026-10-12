package restaurant.site.exception;

// 429 — too many failed attempts, try again later

import lombok.Getter;

@Getter
public class TooManyRequestsException extends RuntimeException {

    private final String code;

    public TooManyRequestsException(String code) {
        super(code);
        this.code = code;
    }
}
