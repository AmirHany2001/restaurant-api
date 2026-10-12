package restaurant.site.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import restaurant.site.dto.UserDto;
import restaurant.site.services.application.UserService;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@NullMarked
public class UserController {
    private final UserService userService;

    @PreAuthorize("hasRole('ADMIN') or @authz.isSelf(#id)")
    @GetMapping("/getUser/{id}")
    public ResponseEntity<UserDto.Response> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    @GetMapping("/getAllUsers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDto.Response>> getAll() {
        return ResponseEntity.ok(userService.getAll());
    }

    @PreAuthorize("hasRole('ADMIN') or @authz.isSelf(#id)")
    @PutMapping("/updateUser/{id}")
    public ResponseEntity<UserDto.Response> update(@PathVariable Long id, @Valid @RequestBody UserDto.UpdateRequest request) {
        return ResponseEntity.ok(userService.update(id, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/changeRole/{id}")
    public ResponseEntity<UserDto.Response> changeRole(@PathVariable Long id,
                                                       @Valid @RequestBody UserDto.RoleRequest request) {
        return ResponseEntity.ok(userService.changeRole(id, request.getRole()));
    }

    @PreAuthorize("hasRole('ADMIN') or @authz.isSelf(#id)")
    @DeleteMapping("/deleteUser/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.accepted().eTag("user.delete").build();

    }



}
