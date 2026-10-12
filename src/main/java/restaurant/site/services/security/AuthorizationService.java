package restaurant.site.services.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import restaurant.site.repo.UserRepository;

// Used inside @PreAuthorize as:  @authz.isSelf(#id)
@Component("authz")
@RequiredArgsConstructor
public class AuthorizationService {

    private final UserRepository userRepository;

    // true if the logged-in user (from the JWT) is the user with this id
    public boolean isSelf(Long userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || userId == null) {
            return false;
        }
        return userRepository.findByEmailIgnoreCase(authentication.getName())
                .map(user -> userId.equals(user.getId()))
                .orElse(false);
    }
}
