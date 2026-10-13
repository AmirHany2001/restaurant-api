package restaurant.site.services.application;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import restaurant.site.dto.PageResponse;
import restaurant.site.dto.UserDto;
import restaurant.site.entity.User;
import restaurant.site.mapper.UserMapper;
import restaurant.site.exception.BusinessLogicException;
import restaurant.site.exception.DuplicateResourceException;
import restaurant.site.exception.NotFoundException;
import restaurant.site.repo.UserRepository;



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


    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "'list:' + #page + ':' + #size")
    public PageResponse<UserDto.Response> getAll(int page, int size) {
        Page<User> users = userRepository.findAll(PageRequest.of(page, size, Sort.by("id")));
        return PageResponse.from(users.map(userMapper::toResponse));
    }


    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "users", allEntries = true),
            @CacheEvict(value = "orders", allEntries = true) // the user's orders are deleted with the user
    })
    public void delete(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("user.not.found", id));

        checkNotLastAdmin(user);
        userRepository.delete(user);
    }

    // ADMIN <-> USER. Only an admin can call this (see UserController).
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public UserDto.Response changeRole(Long id, User.Role role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("user.not.found", id));

        if (user.getRole() == role) {
            throw new BusinessLogicException("user.role.duplicate");
        }

        if (role != User.Role.ADMIN) {
            checkNotLastAdmin(user);
        }

        user.setRole(role);
        return userMapper.toResponse(userRepository.save(user));
    }

    // the system must always keep at least one admin
    private void checkNotLastAdmin(User user) {
        if (user.getRole() == User.Role.ADMIN && userRepository.countByRole(User.Role.ADMIN) <= 1) {
            throw new BusinessLogicException("user.last.admin");
        }
    }


    @Transactional
    @CacheEvict(value = "users", allEntries = true)
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
