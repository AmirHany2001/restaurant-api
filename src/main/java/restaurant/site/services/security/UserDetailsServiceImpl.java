package restaurant.site.services.security;


import restaurant.site.exception.NotFoundException;
import restaurant.site.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@NullMarked
public class UserDetailsServiceImpl implements UserDetailsService {

    public static final String ROLE = "ROLE_";
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername( String email)
            throws NotFoundException {

        return userRepository.findByEmailIgnoreCase(email)
                .map(user -> new org.springframework.security.core.userdetails.User(
                        user.getEmail(),
                        user.getPassword(),
                        List.of(new SimpleGrantedAuthority(
                                ROLE + user.getRole().name()))
                ))
                .orElseThrow(() -> new NotFoundException(
                        "user.not.found.email"));
    }
}
