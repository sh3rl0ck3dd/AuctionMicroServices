package com.example.auctionservice;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<Map<String, Object>> handleResponseStatusException(
      ResponseStatusException ex, HttpServletRequest request) {
    log.warn("{} {} — {} {}: {}",
        request.getMethod(), request.getRequestURI(),
        ex.getStatusCode(), ex.getStatusCode().value(), ex.getReason());
    return ResponseEntity.status(ex.getStatusCode())
        .body(Map.of("error", ex.getReason(), "status", ex.getStatusCode().value()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidation(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    String message = ex.getBindingResult().getFieldErrors().stream()
        .map(e -> e.getField() + ": " + e.getDefaultMessage())
        .collect(Collectors.joining(", "));
    log.warn("{} {} — validation failed: {}", request.getMethod(), request.getRequestURI(), message);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(Map.of("error", message, "status", 400));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> handleException(
      Exception ex, HttpServletRequest request) {
    log.error("{} {} — internal error: {}", request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(Map.of("error", "Internal server error", "status", 500));
  }
}
