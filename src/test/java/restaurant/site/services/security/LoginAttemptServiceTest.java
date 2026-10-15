package restaurant.site.services.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginAttemptServiceTest {

    private final LoginAttemptService attempts = new LoginAttemptService();

    @Test
    void fourFailuresAreAllowed_theFifthBlocks() {
        for (int i = 0; i < 4; i++) attempts.recordFailure("ip|a@x.com");
        assertThat(attempts.isBlocked("ip|a@x.com")).isFalse();

        attempts.recordFailure("ip|a@x.com");

        assertThat(attempts.isBlocked("ip|a@x.com")).isTrue();
    }

    @Test
    void otherKeysAreNotAffected() {
        for (int i = 0; i < 5; i++) attempts.recordFailure("ip|a@x.com");

        assertThat(attempts.isBlocked("ip|b@x.com")).isFalse();
        assertThat(attempts.isBlocked("other-ip|a@x.com")).isFalse();
    }

    @Test
    void resetUnblocks() {
        for (int i = 0; i < 5; i++) attempts.recordFailure("ip|a@x.com");

        attempts.reset("ip|a@x.com");

        assertThat(attempts.isBlocked("ip|a@x.com")).isFalse();
    }

    @Test
    void anUnknownKeyIsNotBlocked() {
        assertThat(attempts.isBlocked("never-seen")).isFalse();
    }
}
