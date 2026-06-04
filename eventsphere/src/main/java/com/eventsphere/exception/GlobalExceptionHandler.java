package com.eventsphere.exception;

import com.eventsphere.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = Logger.getLogger(GlobalExceptionHandler.class.getName());

    public static class BaseException extends RuntimeException {
        private final HttpStatus status;

        public BaseException(String message, HttpStatus status) {
            super(message);
            this.status = status;
        }

        public HttpStatus getStatus() {
            return status;
        }
    }

    public static class ResourceNotFoundException extends BaseException {
        public ResourceNotFoundException(String message) {
            super(message, HttpStatus.NOT_FOUND);
        }
    }

    public static class BadRequestException extends BaseException {
        public BadRequestException(String message) {
            super(message, HttpStatus.BAD_REQUEST);
        }
    }

    public static class ConflictException extends BaseException {
        public ConflictException(String message) {
            super(message, HttpStatus.CONFLICT);
        }
    }

    public static class InvalidCaptchaException extends BaseException {
        public InvalidCaptchaException() {
            super("Invalid CAPTCHA", HttpStatus.BAD_REQUEST);
        }
    }

    public static class InvalidOtpException extends BaseException {
        public InvalidOtpException() {
            super("Invalid or expired OTP", HttpStatus.UNAUTHORIZED);
        }
    }

    public static class EventFullException extends BaseException {
        public EventFullException() {
            super("This event has reached its maximum capacity.", HttpStatus.CONFLICT);
        }
    }

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse.Error> handleBaseException(BaseException ex) {
        return buildError(ex.getStatus(), ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse.Error> handleValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .toList();
        return buildError(HttpStatus.BAD_REQUEST, "Validation failed", errors);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse.Error> handleBadCredentials(BadCredentialsException ex) {
        return buildError(HttpStatus.UNAUTHORIZED, "Invalid username or password", null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse.Error> handleAccessDenied(AccessDeniedException ex) {
        return buildError(HttpStatus.FORBIDDEN, "Access denied", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse.Error> handleGeneralException(Exception ex) {
        LOGGER.log(Level.SEVERE, "Unhandled exception", ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", null);
    }

    private String formatFieldError(FieldError error) {
        return error.getDefaultMessage() != null
                ? error.getDefaultMessage()
                : error.getField() + ": validation failed";
    }

    private ResponseEntity<ApiResponse.Error> buildError(HttpStatus status, String message, List<String> errors) {
        ApiResponse.Error body = new ApiResponse.Error(false, status.value(), message, errors, java.time.LocalDateTime.now());
        return ResponseEntity.status(status).body(body);
    }
}
