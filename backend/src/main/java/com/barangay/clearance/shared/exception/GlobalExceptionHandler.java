package com.barangay.clearance.shared.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidation(
                        MethodArgumentNotValidException ex, HttpServletRequest request) {
                Map<String, String> details = ex.getBindingResult().getFieldErrors().stream()
                                .collect(Collectors.toMap(
                                                FieldError::getField,
                                                fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage()
                                                                : "Invalid value",
                                                (a, b) -> a));
                log.warn("Validation failed at {}: {}", request.getRequestURI(), details);
                return build(HttpStatus.BAD_REQUEST, "Validation failed", request.getRequestURI(), details);
        }

        @ExceptionHandler(ConstraintViolationException.class)
        public ResponseEntity<ErrorResponse> handleConstraintViolation(
                        ConstraintViolationException ex, HttpServletRequest request) {
                Map<String, String> details = ex.getConstraintViolations().stream()
                                .collect(Collectors.toMap(
                                                cv -> cv.getPropertyPath().toString(),
                                                cv -> cv.getMessage(),
                                                (a, b) -> a));
                log.warn("Constraint violation at {}: {}", request.getRequestURI(), details);
                return build(HttpStatus.BAD_REQUEST, "Constraint violation", request.getRequestURI(), details);
        }

        @ExceptionHandler(AppException.class)
        public ResponseEntity<ErrorResponse> handleApp(AppException ex, HttpServletRequest request) {
                log.warn("AppException [{}] at {}: {}", ex.getStatus().value(), request.getRequestURI(),
                                ex.getMessage());
                return build(ex.getStatus(), ex.getMessage(), request.getRequestURI(), null);
        }

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ErrorResponse> handleAccessDenied(
                        AccessDeniedException ex, HttpServletRequest request) {
                log.warn("Access denied at {}: {}", request.getRequestURI(), ex.getMessage());
                return build(HttpStatus.FORBIDDEN, "Access denied", request.getRequestURI(), null);
        }

        @ExceptionHandler(AuthenticationException.class)
        public ResponseEntity<ErrorResponse> handleAuthentication(
                        AuthenticationException ex, HttpServletRequest request) {
                log.warn("Authentication failure at {}: {}", request.getRequestURI(), ex.getMessage());
                return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), request.getRequestURI(), null);
        }

        @ExceptionHandler(RuntimeException.class)
        public ResponseEntity<ErrorResponse> handleRuntime(RuntimeException ex, HttpServletRequest request) {
                log.error("Unhandled exception at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
                return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred",
                                request.getRequestURI(), null);
        }

        private ResponseEntity<ErrorResponse> build(
                        HttpStatus status, String message, String path, Map<String, String> details) {
                ErrorResponse body = ErrorResponse.builder()
                                .status(status.value())
                                .error(status.getReasonPhrase())
                                .message(message)
                                .timestamp(Instant.now())
                                .path(path)
                                .details(details)
                                .build();
                return ResponseEntity.status(status).body(body);
        }

}
