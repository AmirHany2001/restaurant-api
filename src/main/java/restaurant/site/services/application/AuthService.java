package restaurant.site.services.application;


import jakarta.annotation.PostConstruct;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;
import restaurant.site.exception.InvalidCredentialsException;
import restaurant.site.exception.TooManyRequestsException;
import restaurant.site.services.security.LoginAttemptService;
import restaurant.site.services.security.JwtService;
import restaurant.site.dto.AuthDto;
import restaurant.site.dto.UserDto;
import restaurant.site.entity.User;
import restaurant.site.mapper.UserMapper;
import restaurant.site.exception.DuplicateResourceException;
import restaurant.site.exception.NotFoundException;
import restaurant.site.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;


@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;

    // a valid hash of a random value, used when the email does not exist (see login)
    private String dummyHash;

    @PostConstruct
    void initDummyHash() {
        dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional
    @CacheEvict(value = "users", key = "'all'")
    public AuthDto.Response register(UserDto.Request request) {

        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new DuplicateResourceException("user.duplicate.email", request.getEmail());
        }

        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new DuplicateResourceException("user.duplicate.PhoneNumber", request.getPhoneNumber());
        }


        User user = userMapper.toEntity(request);

        user.setPassword(passwordEncoder.encode(request.getPassword()));

        // public registration can only create customers; admins come from AdminInitializer or another admin
        user.setRole(User.Role.USER);

        User savedUser = userRepository.save(user);

        String token = jwtService.generateToken(savedUser);

        return buildResponse(token, savedUser);
    }


    // clientIp + email identify one "login attempt source" for the brute-force limiter
    public AuthDto.Response login(AuthDto.Request request, String clientIp) {

        String attemptKey = clientIp + "|" + request.getEmail().toLowerCase();

        if (loginAttemptService.isBlocked(attemptKey)) {
            throw new TooManyRequestsException("auth.too.many.attempts");
        }


        User user = userRepository.findByEmailIgnoreCase(request.getEmail()).orElse(null);

        // BCrypt is slow on purpose. If an unknown email skipped it, the faster answer would
        // reveal that the email does not exist, so we always compare against some hash.
        String hash = user != null ? user.getPassword() : dummyHash;
        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), hash);

        if (user == null || !passwordMatches) {
            loginAttemptService.recordFailure(attemptKey);
            throw new InvalidCredentialsException("auth.invalid.credentials"); // same answer for both cases
        }

        loginAttemptService.reset(attemptKey);

        String token = jwtService.generateToken(user);

        return buildResponse(token, user);
    }

    // ── Build response ────────────────────────────────────
    private AuthDto.Response buildResponse(String token, User user) {
        AuthDto.Response response = new AuthDto.Response();
        response.setToken(token);
        response.setTokenType("Bearer");
        response.setUserId(user.getId());
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        return response;
    }
}
