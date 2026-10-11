package restaurant.site.services.application;


import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;
import restaurant.site.exception.BusinessLogicException;
import restaurant.site.services.security.JwtService;
import restaurant.site.dto.AuthDto;
import restaurant.site.dto.UserDto;
import restaurant.site.entity.User;
import restaurant.site.mapper.UserMapper;
import restaurant.site.exception.DuplicateResourceException;
import restaurant.site.exception.NotFoundException;
import restaurant.site.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${application.security.admin.secret}")
    private String adminSecret;

    @Transactional
    @CacheEvict(value = "users", key = "'all'")
    public AuthDto.Response register(UserDto.Request request , String secret) {

        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new DuplicateResourceException("user.duplicate.email", request.getEmail());
        }

        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new DuplicateResourceException("user.duplicate.PhoneNumber", request.getPhoneNumber());
        }


        User user = userMapper.toEntity(request);

        user.setPassword(passwordEncoder.encode(request.getPassword()));

        user.setRole(secret.equals(adminSecret) ? User.Role.ADMIN : User.Role.USER);

        User savedUser = userRepository.save(user);

        String token = jwtService.generateToken(savedUser);

        return buildResponse(token, savedUser);
    }


    public AuthDto.Response login(AuthDto.Request request) {


        User user = userRepository.findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> new NotFoundException("user.not.found.email"));


        if(!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessLogicException("user.password.login");
        }

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
