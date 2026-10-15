package restaurant.site.services.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import restaurant.site.dto.UserDto;
import restaurant.site.entity.User;
import restaurant.site.exception.BusinessLogicException;
import restaurant.site.exception.DuplicateResourceException;
import restaurant.site.mapper.UserMapper;
import restaurant.site.repo.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock UserMapper userMapper;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks UserService userService;

    private static User user(long id, User.Role role) {
        User user = new User();
        user.setId(id);
        user.setFullName("Old Name");
        user.setEmail("user" + id + "@example.com");
        user.setPassword("old-hash");
        user.setPhoneNumber("01000000000");
        user.setAddress("Old address");
        user.setRole(role);
        return user;
    }

    // ---------- roles: the system must always keep an admin ----------

    @Test
    void changeRole_cannotDemoteTheLastAdmin() {
        User admin = user(1, User.Role.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.countByRole(User.Role.ADMIN)).thenReturn(1L);

        Throwable thrown = catchThrowable(() -> userService.changeRole(1L, User.Role.USER));

        assertThat(thrown).isInstanceOf(BusinessLogicException.class).hasFieldOrPropertyWithValue("code", "user.last.admin");
        assertThat(admin.getRole()).isEqualTo(User.Role.ADMIN);
        verify(userRepository, never()).save(any());
    }

    @Test
    void changeRole_canDemoteAnAdminWhenAnotherOneRemains() {
        User admin = user(1, User.Role.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.countByRole(User.Role.ADMIN)).thenReturn(2L);
        when(userRepository.save(admin)).thenReturn(admin);

        userService.changeRole(1L, User.Role.USER);

        assertThat(admin.getRole()).isEqualTo(User.Role.USER);
    }

    @Test
    void changeRole_promotingNeverNeedsTheAdminCount() {
        User customer = user(2, User.Role.USER);
        when(userRepository.findById(2L)).thenReturn(Optional.of(customer));
        when(userRepository.save(customer)).thenReturn(customer);

        userService.changeRole(2L, User.Role.ADMIN);

        assertThat(customer.getRole()).isEqualTo(User.Role.ADMIN);
        verify(userRepository, never()).countByRole(any());
    }

    @Test
    void changeRole_sameRoleIsRefused() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2, User.Role.USER)));

        Throwable thrown = catchThrowable(() -> userService.changeRole(2L, User.Role.USER));

        assertThat(thrown).isInstanceOf(BusinessLogicException.class).hasFieldOrPropertyWithValue("code", "user.role.duplicate");
    }

    // ---------- delete ----------

    @Test
    void delete_cannotRemoveTheLastAdmin() {
        User admin = user(1, User.Role.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.countByRole(User.Role.ADMIN)).thenReturn(1L);

        Throwable thrown = catchThrowable(() -> userService.delete(1L));

        assertThat(thrown).isInstanceOf(BusinessLogicException.class).hasFieldOrPropertyWithValue("code", "user.last.admin");
        verify(userRepository, never()).delete(any());
    }

    @Test
    void delete_removesAnOrdinaryUser() {
        User customer = user(2, User.Role.USER);
        when(userRepository.findById(2L)).thenReturn(Optional.of(customer));

        userService.delete(2L);

        verify(userRepository).delete(customer);
    }

    // ---------- update: only what is sent changes ----------

    @Test
    void update_changesOnlyTheFieldsThatWereSent() {
        User customer = user(2, User.Role.USER);
        when(userRepository.findById(2L)).thenReturn(Optional.of(customer));
        when(userRepository.save(customer)).thenReturn(customer);

        UserDto.UpdateRequest request = new UserDto.UpdateRequest();
        request.setFullName("New Name");
        request.setAddress("   "); // blank = "not sent"

        userService.update(2L, request);

        assertThat(customer.getFullName()).isEqualTo("New Name");
        assertThat(customer.getAddress()).isEqualTo("Old address");
        assertThat(customer.getEmail()).isEqualTo("user2@example.com");
        assertThat(customer.getPhoneNumber()).isEqualTo("01000000000");
        assertThat(customer.getPassword()).isEqualTo("old-hash");
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void update_refusesAnEmailThatBelongsToSomeoneElse() {
        User customer = user(2, User.Role.USER);
        when(userRepository.findById(2L)).thenReturn(Optional.of(customer));
        when(userRepository.existsByEmailIgnoreCase("taken@example.com")).thenReturn(true);

        UserDto.UpdateRequest request = new UserDto.UpdateRequest();
        request.setEmail("taken@example.com");

        Throwable thrown = catchThrowable(() -> userService.update(2L, request));

        assertThat(thrown).isInstanceOf(DuplicateResourceException.class).hasFieldOrPropertyWithValue("code", "user.duplicate.email");
        assertThat(customer.getEmail()).isEqualTo("user2@example.com");
    }

    @Test
    void update_changingTheEmailOnlyByCaseIsNotACollisionWithYourself() {
        User customer = user(2, User.Role.USER);
        when(userRepository.findById(2L)).thenReturn(Optional.of(customer));
        when(userRepository.save(customer)).thenReturn(customer);

        UserDto.UpdateRequest request = new UserDto.UpdateRequest();
        request.setEmail("USER2@example.com");

        userService.update(2L, request);

        verify(userRepository, never()).existsByEmailIgnoreCase(anyString()); // same address, no duplicate check
    }
}
