package restaurant.site.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import restaurant.site.entity.User;
import restaurant.site.repo.UserRepository;

// Runs once at every startup. If ADMIN-EMAIL and ADMIN-PASSWORD are set:
//   - no user with that email   -> creates an ADMIN
//   - user exists, not an admin -> promotes it to ADMIN
//   - user already an admin     -> does nothing (the password is never reset)
// Later admins are managed by existing admins through PUT /users/changeRole/{id}.
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CacheManager cacheManager;

    @Value("${application.security.admin.email:}")
    private String adminEmail;

    @Value("${application.security.admin.password:}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        if (!StringUtils.hasText(adminEmail) || !StringUtils.hasText(adminPassword)) {
            log.warn("ADMIN-EMAIL / ADMIN-PASSWORD are not set: no admin is created at startup");
            return;
        }

        if (adminPassword.length() < 8) {
            throw new IllegalStateException("ADMIN-PASSWORD must be at least 8 characters");
        }

        User user = userRepository.findByEmailIgnoreCase(adminEmail).orElse(null);

        if (user == null) {
            user = new User();
            user.setFullName("Administrator");
            user.setEmail(adminEmail);
            user.setPassword(passwordEncoder.encode(adminPassword));
            user.setPhoneNumber("-");   // required columns; the admin can change them later
            user.setAddress("-");
            user.setRole(User.Role.ADMIN);
            userRepository.save(user);
            log.info("Admin user created: {}", adminEmail);
        } else if (user.getRole() != User.Role.ADMIN) {
            user.setRole(User.Role.ADMIN);
            userRepository.save(user);
            log.info("Existing user promoted to admin: {}", adminEmail);
        } else {
            return; // nothing changed, nothing to clear
        }

        // cached user lists (Redis outlives the app) are now out of date
        try {
            Cache users = cacheManager.getCache("users");
            if (users != null) {
                users.clear();
            }
        } catch (RuntimeException e) {
            log.warn("Could not clear the users cache: {}", e.getMessage());
        }
    }
}
