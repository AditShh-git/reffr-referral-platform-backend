package com.Reffr_Backend.common.exception;

import com.Reffr_Backend.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import jakarta.validation.ConstraintViolationException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── Single handler for the entire BaseException hierarchy ─────────
    // Covers: BusinessException(400), NotFoundException(404),
    //         ForbiddenException(403), UnauthorizedException(401)
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleBase(
            BaseException ex, HttpServletRequest request) {

        boolean isServerError = ex.getStatus().is5xxServerError();
        String action = request.getMethod() + " " + request.getRequestURI();
        if (isServerError) {
            log.error("[userId={}] {} | {} | code={} message={}",
                    userId(), ex.getStatus(), action, ex.getCode(), ex.getMessage(), ex);
        } else {
            log.warn("[userId={}] {} | {} | code={} message={}",
                    userId(), ex.getStatus(), action, ex.getCode(), ex.getMessage());
        }

        return ResponseEntity.status(ex.getStatus())
                .body(ApiResponse.error(ex.getCode(), ex.getMessage(), ex.getStatus()));
    }

    // ── 400 Validation Errors (Bean Validation) ───────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(err -> {
            String field   = ((FieldError) err).getField();
            String message = err.getDefaultMessage();
            fieldErrors.put(field, message);
        });

        log.warn("[{}] VALIDATION_FAILED | {} {} | fields={}",
                requestId(), request.getMethod(), request.getRequestURI(),
                fieldErrors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCodes.INVALID_REQUEST, "Validation failed", 
                        HttpStatus.BAD_REQUEST, fieldErrors));
    }

    // ── 401 Spring Security AuthenticationException ───────────────────
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(
            AuthenticationException ex, HttpServletRequest request) {

        log.warn("[{}] UNAUTHENTICATED | {} {}",
                requestId(), request.getMethod(), request.getRequestURI());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ErrorCodes.UNAUTHORIZED, "Authentication required",
                        HttpStatus.UNAUTHORIZED));
    }

    // ── 403 Spring Security AccessDeniedException ─────────────────────
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {

        log.warn("[{}] ACCESS_DENIED | {} {}",
                requestId(), request.getMethod(), request.getRequestURI());

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ErrorCodes.ACCESS_DENIED, "Access denied", HttpStatus.FORBIDDEN));
    }

    // ── 409 Optimistic Locking ────────────────────────────────────────
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLocking(
            ObjectOptimisticLockingFailureException ex, HttpServletRequest request) {

        log.warn("[{}] CONCURRENCY_CONFLICT | {} {}",
                requestId(), request.getMethod(), request.getRequestURI());

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ErrorCodes.CONCURRENCY_CONFLICT,
                        "Resource was modified by another transaction. Please refresh and try again.",
                        HttpStatus.CONFLICT));
    }

    // ── 413 File Too Large ────────────────────────────────────────────
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleFileSize(
            MaxUploadSizeExceededException ex, HttpServletRequest request) {

        log.warn("[{}] FILE_TOO_LARGE | {} {}",
                requestId(), request.getMethod(), request.getRequestURI());

        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.error(ErrorCodes.FILE_TOO_LARGE, "File exceeds 5MB limit",
                        HttpStatus.PAYLOAD_TOO_LARGE));
    }

    // ── 400 Illegal argument / State ──────────────────────────────────
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(
            RuntimeException ex, HttpServletRequest request) {

        log.warn("[{}] BAD_REQUEST | {} {} | {}",
                requestId(), request.getMethod(), request.getRequestURI(),
                ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCodes.INVALID_REQUEST, ex.getMessage(),
                        HttpStatus.BAD_REQUEST));
    }

    // ── 400 Data Integrity ────────────────────────────────────────────
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(
            DataIntegrityViolationException ex, HttpServletRequest request) {

        String message = "Database error: Data integrity violation";
        String rootMsg = ex.getRootCause() != null ? ex.getRootCause().getMessage() : "";

        if (rootMsg.contains("user_skills_user_id_skill_key") || rootMsg.contains("uk_user_skills_user_id_skill_name")) {
            message = "Duplicate skill not allowed";
        } else if (rootMsg.contains("uk_") || rootMsg.contains("unique constraint")) {
            message = "A record with this information already exists";
        }

        log.warn("[{}] DATA_INTEGRITY_VIOLATION | {} {} | {}",
                requestId(), request.getMethod(), request.getRequestURI(), rootMsg);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCodes.INVALID_REQUEST, message,
                        HttpStatus.BAD_REQUEST));
    }

    // ── 500 Unexpected ────────────────────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(
            Exception ex, HttpServletRequest request) {

        log.error("[{}] UNEXPECTED_ERROR | {} {} | type={} message={}",
                requestId(), request.getMethod(), request.getRequestURI(),
                ex.getClass().getSimpleName(), ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorCodes.GENERIC_ERROR,
                        "An unexpected error occurred. Request ID: " + requestId(),
                        HttpStatus.INTERNAL_SERVER_ERROR));
    }

    // ── Helper ────────────────────────────────────────────────────────
    private String requestId() {
        return MDC.get("requestId") != null ? MDC.get("requestId") : "no-id";
    }

    private String userId() {
        return MDC.get("userId") != null ? MDC.get("userId") : "anonymous";
    }
}
