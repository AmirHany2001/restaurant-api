package restaurant.site.config.Security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import restaurant.site.exception.ErrorResponse;
import restaurant.site.services.language.ChooseLanguageService;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

// Security filters run BEFORE Spring MVC, so @RestControllerAdvice never sees their errors.
// These two handlers write the same JSON error body (and language) as the rest of the API.
@Component
@RequiredArgsConstructor
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final ChooseLanguageService chooseLanguageService;

    // no token / invalid token / expired token on a protected URL -> 401
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        write(response, HttpStatus.UNAUTHORIZED, "error.unauthorized");
    }

    // logged in, but the role is not allowed for this URL -> 403
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        write(response, HttpStatus.FORBIDDEN, "error.access.denied");
    }

    private void write(HttpServletResponse response, HttpStatus status, String messageCode) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(),
                new ErrorResponse(status.value(),
                        chooseLanguageService.getAcceptedLanguageMessage(messageCode), null));
    }
}
