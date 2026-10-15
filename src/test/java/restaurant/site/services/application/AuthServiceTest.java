package restaurant.site.services.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import restaurant.site.dto.AuthDto;
import restaurant.site.dto.UserDto;
import restaurant.site.entity.User;
import restaurant.site.exception.InvalidCredentialsException;
import restaurant.site.exception.TooManyRequestsException;
import restaurant.site.mapper.UserMapper;
import restaurant.site.repo.UserRepository;
import restaurant.site.services.security.JwtService;
import restaurant.site.services.security.LoginAttemptService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// The LoginAttemptService is the REAL one: the point of these tests is how login and the limiter work together.
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String IP = "10.0.0.1";

    @Mock JwtService jwtService;
    @Mock UserRepository userRepository;
    @Mock UserMapper userMapper;
    @Mock PasswordEncoder passwordEncoder;

    private final LoginAttemptService attempts = new LoginAttemptService();
    private AuthService authService;

    @BeforeEach
    void setUp() {
        when(passwordEncoder.encode(anyString())).thenReturn("dummy-hash");
        authService = new AuthService(jwtService, userRepository, userMapper, passwordEncoder, attempts);
        authService.initDummyHash(); // normally done by @PostConstruct
    }

    private static AuthDto.Request credentials(String email, String password) {
        AuthDto.Request request = new AuthDto.Request();
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    private User sara() {
        User user = new User();
        user.setId(7L);
        user.setEmail("sara@example.com");
        user.setPassword("real-hash");
        user.setRole(User.Role.USER);
        return user;
    }

    // ---------- login: no hint about which emails exist ----------

    @Test
    void login_unknownEmailAndWrongPasswordGiveTheSameAnswer() {
        when(userRepository.findByEmailIgnoreCase("ghost@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("sara@example.com")).thenReturn(Optional.of(sara()));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        Throwable unknownEmail = catchThrowable(() -> authService.login(credentials("ghost@example.com", "pw"), IP));
        Throwable wrongPassword = catchThrowable(() -> authService.login(credentials("sara@example.com", "pw"), IP));

        assertThat(unknownEmail).isInstanceOf(InvalidCredentialsException.class);
        assertThat(wrongPassword).isInstanceOf(InvalidCredentialsException.class);
        assertThat(((InvalidCredentialsException) unknownEmail).getCode())
                .isEqualTo(((InvalidCredentialsException) wrongPassword).getCode());
    }

    @Test
    void login_unknownEmailStillPaysForAPasswordCheck_soTheResponseTimeDoesNotGiveItAway() {
        when(userRepository.findByEmailIgnoreCase("ghost@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.matches("pw", "dummy-hash")).thenReturn(false);

        catchThrowable(() -> authService.login(credentials("ghost@example.com", "pw"), IP));

        verify(passwordEncoder).matches("pw", "dummy-hash"); // compared against a dummy hash, like a real account
    }

    @Test
    void login_succeedsWithTheRightPassword() {
        User user = sara();
        when(userRepository.findByEmailIgnoreCase("sara@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("good", "real-hash")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("the-token");

        AuthDto.Response response = authService.login(credentials("sara@example.com", "good"), IP);

        assertThat(response.getToken()).isEqualTo("the-token");
        assertThat(response.getRole()).isEqualTo(User.Role.USER);
    }

    // ---------- login: brute-force limiter ----------

    @Test
    void login_isBlockedAfterFiveWrongPasswords_evenIfTheSixthIsCorrect() {
        when(userRepository.findByEmailIgnoreCase("sara@example.com")).thenReturn(Optional.of(sara()));
        when(passwordEncoder.matches("bad", "real-hash")).thenReturn(false);
        for (int i = 0; i < 5; i++) {
            catchThrowable(() -> authService.login(credentials("sara@example.com", "bad"), IP));
        }

        Throwable sixth = catchThrowable(() -> authService.login(credentials("sara@example.com", "good"), IP));

        assertThat(sixth).isInstanceOf(TooManyRequestsException.class);
        verify(jwtService, never()).generateToken(any()); // no token was issued
    }

    @Test
    void login_aSuccessfulLoginResetsTheCounter() {
        User user = sara();
        when(userRepository.findByEmailIgnoreCase("sara@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bad", "real-hash")).thenReturn(false);
        when(passwordEncoder.matches("good", "real-hash")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("tok");

        for (int i = 0; i < 4; i++) catchThrowable(() -> authService.login(credentials("sara@example.com", "bad"), IP));
        authService.login(credentials("sara@example.com", "good"), IP); // success: counter back to zero
        for (int i = 0; i < 4; i++) catchThrowable(() -> authService.login(credentials("sara@example.com", "bad"), IP));

        // 4 + 4 failures in total would be blocked; after a reset it is still just "wrong password"
        Throwable next = catchThrowable(() -> authService.login(credentials("sara@example.com", "bad"), IP));
        assertThat(next).isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_theLimitIsPerIp_soAnAttackerCannotLockOutTheRealOwner() {
        when(userRepository.findByEmailIgnoreCase("sara@example.com")).thenReturn(Optional.of(sara()));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);
        for (int i = 0; i < 5; i++) {
            catchThrowable(() -> authService.login(credentials("sara@example.com", "bad"), "6.6.6.6")); // attacker
        }

        Throwable attacker = catchThrowable(() -> authService.login(credentials("sara@example.com", "bad"), "6.6.6.6"));
        Throwable owner = catchThrowable(() -> authService.login(credentials("sara@example.com", "bad"), "10.0.0.1"));

        assertThat(attacker).isInstanceOf(TooManyRequestsException.class);
        assertThat(owner).isInstanceOf(InvalidCredentialsException.class); // not blocked
    }

    // ---------- register ----------

    @Test
    void register_alwaysCreatesAnOrdinaryUser_withAnEncodedPassword() {
        UserDto.Request request = new UserDto.Request();
        request.setEmail("new@example.com");
        request.setPhoneNumber("01012345678");
        request.setPassword("Password1!");
        when(userRepository.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("01012345678")).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(new User());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(any(User.class))).thenReturn("tok");

        authService.register(request);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getRole()).isEqualTo(User.Role.USER); // there is no way to ask for ADMIN here
        assertThat(saved.getValue().getPassword()).isEqualTo("dummy-hash").isNotEqualTo("Password1!");
    }
}
