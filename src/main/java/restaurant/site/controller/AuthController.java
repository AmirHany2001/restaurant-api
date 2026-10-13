package restaurant.site.controller;


import restaurant.site.dto.AuthDto;
import restaurant.site.dto.UserDto;
import restaurant.site.services.application.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
    public ResponseEntity<AuthDto.Response> register(
            @Valid @RequestBody UserDto.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDto.Response> login(@Valid @RequestBody AuthDto.Request request,
                                                           HttpServletRequest httpRequest) {
        // behind a reverse proxy this is the proxy address unless forward headers are configured
        return ResponseEntity.ok(authService.login(request, httpRequest.getRemoteAddr()));
    }


}
