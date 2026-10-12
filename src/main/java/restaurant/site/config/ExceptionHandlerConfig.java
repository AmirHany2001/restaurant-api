package restaurant.site.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import restaurant.site.exception.*;
import restaurant.site.services.language.ChooseLanguageService;

import java.util.HashMap;
import java.util.Map;

// Rule for this class: the client only ever receives OUR translated messages.
// Never pass exception.getMessage() of a framework exception to the client: it can contain
// class names, SQL, JSON internals. Details go to the log, not to the response.
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
@NullMarked
public class ExceptionHandlerConfig {

    private final ChooseLanguageService chooseLanguageService;

    // @Valid failed on a DTO field
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handelDtoException(MethodArgumentNotValidException exception) {

        Map<String, String> errors = new HashMap<>();

        exception.getBindingResult()
                .getFieldErrors()
                .forEach(error -> errors.put(error.getField(),
                        chooseLanguageService.getAcceptedLanguageMessage(error.getDefaultMessage())));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(),
                        chooseLanguageService.getAcceptedLanguageMessage("error.validation"), errors));
    }

    // constraint failed on a @PathVariable / @RequestParam (e.g. @NotBlank String name)
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleParameterValidation(HandlerMethodValidationException exception) {

        Map<String, String> errors = new HashMap<>();

        exception.getParameterValidationResults().forEach(result ->
                result.getResolvableErrors().forEach(error ->
                        errors.put(String.valueOf(result.getMethodParameter().getParameterName()),
                                chooseLanguageService.getAcceptedLanguageMessage(error.getDefaultMessage()))));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(),
                        chooseLanguageService.getAcceptedLanguageMessage("error.validation"), errors));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(NotFoundException exception) {
        return build(HttpStatus.NOT_FOUND,
                chooseLanguageService.getAcceptedLanguageMessage(exception.getCode(), exception.getArgs()));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(DuplicateResourceException exception) {
        return build(HttpStatus.CONFLICT,
                chooseLanguageService.getAcceptedLanguageMessage(exception.getCode(), exception.getArgs()));
    }

    @ExceptionHandler(BusinessLogicException.class)
    public ResponseEntity<ErrorResponse> handleBusinessLogic(BusinessLogicException exception) {
        return build(HttpStatus.BAD_REQUEST,
                chooseLanguageService.getAcceptedLanguageMessage(exception.getCode(), exception.getArgs()));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException exception) {
        return build(HttpStatus.FORBIDDEN,
                chooseLanguageService.getAcceptedLanguageMessage(exception.getCode(), exception.getArgs()));
    }

    // A unique / foreign-key / not-null rule of the database was broken and no service check caught it
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException exception) {
        log.warn("Data integrity violation", exception);
        return build(HttpStatus.CONFLICT,
                chooseLanguageService.getAcceptedLanguageMessage("error.data.conflict"));
    }

    // Spring Security thrown from a controller (@PreAuthorize): token missing or invalid
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException exception) {
        return build(HttpStatus.UNAUTHORIZED,
                chooseLanguageService.getAcceptedLanguageMessage("error.unauthorized"));
    }

    // Spring Security thrown from a controller (@PreAuthorize): the user does not have the required role
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException exception) {
        return build(HttpStatus.FORBIDDEN,
                chooseLanguageService.getAcceptedLanguageMessage("error.access.denied"));
    }

    // URL does not exist
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoEndpoint(NoResourceFoundException exception) {
        return build(HttpStatus.NOT_FOUND,
                chooseLanguageService.getAcceptedLanguageMessage("error.not.found"));
    }

    // Malformed JSON, missing request body, or a value that does not fit the field (e.g. a wrong enum)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException exception) {
        log.debug("Unreadable request body", exception);
        return build(HttpStatus.BAD_REQUEST,
                chooseLanguageService.getAcceptedLanguageMessage("error.malformed.request"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed() {
        return build(HttpStatus.METHOD_NOT_ALLOWED,
                chooseLanguageService.getAcceptedLanguageMessage("error.method.not.allowed"));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaType() {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                chooseLanguageService.getAcceptedLanguageMessage("error.unsupported.media.type"));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException exception) {
        return build(HttpStatus.BAD_REQUEST,
                chooseLanguageService.getAcceptedLanguageMessage("error.missing.parameter", exception.getParameterName()));
    }

    // Wrong type in the URL, e.g. /orders/getOrdersByStatus/banana for an enum
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return build(HttpStatus.BAD_REQUEST,
                chooseLanguageService.getAcceptedLanguageMessage("error.invalid.parameter", exception.getName()));
    }

    // Safety net: anything not handled above is a bug on our side -> 500 with a generic message.
    // The full stack trace is logged; the client never sees it. (More specific handlers win over this one.)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
        log.error("Unhandled exception", exception);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                chooseLanguageService.getAcceptedLanguageMessage("error.internal"));
    }

    // ── Private helper — avoids repeating the same 5 lines ──

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message) {
        return ResponseEntity
                .status(status)
                .body(new ErrorResponse(
                        status.value(),
                        message,
                        null
                ));
    }

}
