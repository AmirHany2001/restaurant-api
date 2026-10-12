package restaurant.site.services.application;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import restaurant.site.dto.UserDto;
import restaurant.site.entity.User;
import restaurant.site.mapper.UserMapper;
import restaurant.site.exception.DuplicateResourceException;
import restaurant.site.exception.NotFoundException;
import restaurant.site.repo.UserRepository;

import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Cacheable(value = "users", key = "#id")
    public UserDto.Response getById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("user.not.found", id));
        return userMapper.toResponse(user);
    }


    @Cacheable(value = "users", key = "'all'")
    public List<UserDto.Response> getAll() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toResponse)
                .collect(Collectors.toList());
    }


    @CacheEvict(value = "users", allEntries = true)
    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new NotFoundException("user.not.found", id);
        }
        userRepository.deleteById(id);
    }


    @Transactional
    @Caching(
            put = @CachePut(value = "users", key = "#id"),
            evict = @CacheEvict(value = "users", key = "'all'")
    )
    public UserDto.Response update(Long id, UserDto.UpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("user.not.found", id));


        // hasText = not null and not blank, so "" or "   " means "not sent"
        if (StringUtils.hasText(request.getFullName())) {
            user.setFullName(request.getFullName());
        }

        if (StringUtils.hasText(request.getEmail()) && !request.getEmail().equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
                throw new DuplicateResourceException("user.duplicate.email", request.getEmail());
            }
            user.setEmail(request.getEmail());
        }

        if (StringUtils.hasText(request.getPhoneNumber()) && !request.getPhoneNumber().equals(user.getPhoneNumber())) {
            if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
                throw new DuplicateResourceException("user.duplicate.PhoneNumber", request.getPhoneNumber());
            }
            user.setPhoneNumber(request.getPhoneNumber());
        }


        if (StringUtils.hasText(request.getPassword()) && !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }


        if (StringUtils.hasText(request.getAddress()) && !request.getAddress().equals(user.getAddress())) {
            user.setAddress(request.getAddress());
        }

        return userMapper.toResponse(userRepository.save(user));


    }


}
