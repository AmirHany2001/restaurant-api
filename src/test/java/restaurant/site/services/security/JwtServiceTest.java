package restaurant.site.services.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import restaurant.site.entity.User;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Plain unit test: no Spring context, no database, no Redis.
class JwtServiceTest {

    private static final String GOOD_SECRET = Base64.getEncoder().encodeToString(new byte[48]);

    private JwtService serviceWith(String secret, long expiration) {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "secretKey", secret);
        ReflectionTestUtils.setField(service, "expiration", expiration);
        return service;
    }

    private User user() {
        User user = new User();
        user.setEmail("a@b.com");
        user.setRole(User.Role.ADMIN);
        return user;
    }

    @Test
    void tokenRoundTrip_readsBackEmailAndRole() {
        JwtService service = serviceWith(GOOD_SECRET, 60_000);
        ReflectionTestUtils.invokeMethod(service, "init");

        String token = service.generateToken(user());

        assertEquals("a@b.com", service.extractEmail(token));
        assertEquals("ADMIN", service.extractRole(token));
        assertTrue(service.isTokenValid(token, "a@b.com"));
    }

    @Test
    void tamperedToken_isRejected() {
        JwtService service = serviceWith(GOOD_SECRET, 60_000);
        ReflectionTestUtils.invokeMethod(service, "init");

        String token = service.generateToken(user());

        // change the first character of the signature (the part after the last dot)
        int signatureStart = token.lastIndexOf('.') + 1;
        char first = token.charAt(signatureStart);
        String tampered = token.substring(0, signatureStart) + (first == 'A' ? 'B' : 'A')
                + token.substring(signatureStart + 1);

        assertThrows(JwtException.class, () -> service.extractEmail(tampered));
    }

    @Test
    void shortSecret_failsAtStartup() {
        JwtService service = serviceWith(Base64.getEncoder().encodeToString(new byte[16]), 60_000);

        assertThrows(IllegalStateException.class, () -> ReflectionTestUtils.invokeMethod(service, "init"));
    }

    @Test
    void secretThatIsNotBase64_failsAtStartup() {
        JwtService service = serviceWith("not base64 !!!", 60_000);

        assertThrows(IllegalStateException.class, () -> ReflectionTestUtils.invokeMethod(service, "init"));
    }
}
