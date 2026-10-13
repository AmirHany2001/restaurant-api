package restaurant.site.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import restaurant.site.dto.MessageResponse;
import restaurant.site.dto.UserDto;
import restaurant.site.services.application.UserService;
import restaurant.site.services.language.ChooseLanguageService;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@NullMarked
public class UserController {
    private final UserService userService;
    private final ChooseLanguageService chooseLanguageService;

    @PreAuthorize("hasRole('ADMIN') or @authz.isSelf(#id)")
    @GetMapping("/{id}")
    public ResponseEntity<UserDto.Response> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<UserDto.Response>> getAll() {
        return ResponseEntity.ok(userService.getAll());
    }

    // only the fields that are sent are changed
    @PreAuthorize("hasRole('ADMIN') or @authz.isSelf(#id)")
    @PutMapping("/{id}")
    public ResponseEntity<UserDto.Response> update(@PathVariable Long id, @Valid @RequestBody UserDto.UpdateRequest request) {
        return ResponseEntity.ok(userService.update(id, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/role")
    public ResponseEntity<UserDto.Response> changeRole(@PathVariable Long id,
                                                       @Valid @RequestBody UserDto.RoleRequest request) {
        return ResponseEntity.ok(userService.changeRole(id, request.getRole()));
    }

    @PreAuthorize("hasRole('ADMIN') or @authz.isSelf(#id)")
    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.ok(new MessageResponse(
                chooseLanguageService.getAcceptedLanguageMessage("user.delete")));
    }

}
