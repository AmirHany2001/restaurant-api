package restaurant.site.exception;

// 401 — wrong email or password
// Always the same message for "unknown email" and "wrong password",
// so the response does not reveal which emails are registered.

import lombok.Getter;

@Getter
public class InvalidCredentialsException extends RuntimeException {

    private final String code;

    public InvalidCredentialsException(String code) {
        super(code);
        this.code = code;
    }
}
