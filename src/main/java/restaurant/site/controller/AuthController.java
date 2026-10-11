package restaurant.site.controller;


import restaurant.site.dto.AuthDto;
import restaurant.site.dto.UserDto;
import restaurant.site.services.application.AuthService;
import jakarta.validation.Valid;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;


    @PostMapping("/register")
    public ResponseEntity<AuthDto.@NonNull Response> register(
            @Valid @RequestBody UserDto.Request request ,
            @RequestHeader(
                    value = "secret",
                    required = false,
                    defaultValue = "") String secret) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.register(request , secret));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDto.@NonNull Response> login(@Valid @RequestBody AuthDto.Request request) {
        return ResponseEntity.ok(authService.login(request));
    }


}
