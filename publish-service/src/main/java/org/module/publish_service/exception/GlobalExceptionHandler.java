package org.module.publish_service.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import lombok.extern.slf4j.XSlf4j;
import org.module.publish_service.response.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author : Ly LeangSeng
 * @email : lyleangseng712@gmail.com
 * @date : 10/4/2026 6:02 PM
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationException(MethodArgumentNotValidException ex) {

        Map<String, Object> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        ApiErrorResponse response = new ApiErrorResponse(HttpStatus.BAD_REQUEST, "Validation failed", LocalDateTime.now(), errors);
        return ResponseEntity.badRequest().body(response);
    }


    public ApiErrorResponse apiErrorResponse(String message, HttpStatus status, Map<String, Object> error) {
        return new ApiErrorResponse(status, message, LocalDateTime.now(), error);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {

        ApiErrorResponse response = apiErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND, null);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(ConflictException ex) {

        ApiErrorResponse response = apiErrorResponse(ex.getMessage(), HttpStatus.CONFLICT, null);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {

        ApiErrorResponse response = new ApiErrorResponse(HttpStatus.CONFLICT, "Data already exists", LocalDateTime.now(), null);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleEntityNotFound(EntityNotFoundException ex) {

        ApiErrorResponse response = new ApiErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), LocalDateTime.now(), null);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
    /** @Validated on path/query params failed. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraint(ConstraintViolationException ex) {
        Map<String, Object> errors = new LinkedHashMap<>();
        ex.getConstraintViolations()
                .forEach(v -> errors.putIfAbsent(v.getPropertyPath().toString(), v.getMessage()));

        ApiErrorResponse response = apiErrorResponse("Invalid request parameters", HttpStatus.BAD_REQUEST, errors);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<?> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {

        ApiErrorResponse response = new ApiErrorResponse(HttpStatus.BAD_REQUEST, "HTTP method %s is not supported".formatted(ex.getMethod()), LocalDateTime.now(), null);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        ApiErrorResponse response = apiErrorResponse("Missing required parameter: " + ex.getParameterName(), HttpStatus.BAD_REQUEST, null);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        ApiErrorResponse response = apiErrorResponse("Invalid value for parameter: " + ex.getName(), HttpStatus.BAD_REQUEST, null);
        return ResponseEntity.badRequest()
                .body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        ApiErrorResponse response = apiErrorResponse("Malformed JSON request body", HttpStatus.BAD_REQUEST, null);
        log.debug("Unreadable request body", ex);
        return ResponseEntity.badRequest().body(response);
    }

}
