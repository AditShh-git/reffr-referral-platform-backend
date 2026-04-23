package com.Reffr_Backend.common.response;

import com.Reffr_Backend.common.exception.ErrorCodes;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Map;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final String  message;
    private final T       data;
    private final String  error;     // human-readable error message
    private final String  code;      // machine-readable error code
    private final int     status;
    private final Instant timestamp;

    private ApiResponse(boolean success, String message, T data,
                        String error, String code, int status) {
        this.success   = success;
        this.message   = message;
        this.data      = data;
        this.error     = error;
        this.code      = code;
        this.status    = status;
        this.timestamp = Instant.now();
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, null, data, null, null, HttpStatus.OK.value());
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, null, null, HttpStatus.OK.value());
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(true, null, data, null, null, HttpStatus.CREATED.value());
    }

    /** Used by BaseException handler — exposes machine-readable code in body. */
    public static <T> ApiResponse<T> error(String code, String message, HttpStatus status) {
        return new ApiResponse<>(false, null, null, message, code, status.value());
    }

    /** Used for structured errors like validation failures that carry data. */
    public static <T> ApiResponse<T> error(String code, String message, HttpStatus status, T data) {
        return new ApiResponse<>(false, null, data, message, code, status.value());
    }

    /** Legacy overload — kept for handlers that don't have a domain code. */
    public static <T> ApiResponse<T> error(String message, HttpStatus status) {
        return new ApiResponse<>(false, null, null, message, null, status.value());
    }

    /** @deprecated Use {@link #error(String, String, HttpStatus, Object)} instead. */
    @Deprecated
    public static ApiResponse<Map<String, String>> validationError(
            Map<String, String> fieldErrors) {
        return error(ErrorCodes.INVALID_REQUEST, "Validation failed", HttpStatus.BAD_REQUEST, fieldErrors);
    }
}