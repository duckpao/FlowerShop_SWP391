package com.example.flowershop.exception;

import com.example.flowershop.dto.common.MessageResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<MessageResponse> handleStatus(org.springframework.web.server.ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).body(new MessageResponse(
                exception.getReason() == null ? "Yêu cầu không thể thực hiện." : exception.getReason()));
    }

    @ExceptionHandler({org.springframework.security.core.AuthenticationException.class,
            org.springframework.security.oauth2.jwt.JwtException.class})
    public ResponseEntity<MessageResponse> handleAuthentication(Exception exception) {
        return ResponseEntity.status(401).body(new MessageResponse("Thông tin đăng nhập hoặc phiên không hợp lệ."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException exception
    ) {
        Map<String, String> errors = new LinkedHashMap<>();

        exception.getBindingResult()
                .getFieldErrors()
                .forEach(error -> errors.putIfAbsent(
                        error.getField(),
                        error.getDefaultMessage()
                ));

        return ResponseEntity.badRequest().body(
                Map.of(
                        "message", "Dữ liệu không hợp lệ",
                        "errors", errors
                )
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<MessageResponse> handleInvalidArgument(
            IllegalArgumentException exception
    ) {
        return ResponseEntity.badRequest().body(
                new MessageResponse(exception.getMessage())
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<MessageResponse> handleDataConflict(
            DataIntegrityViolationException exception
    ) {
        return ResponseEntity.status(409).body(
                new MessageResponse(
                        "Dữ liệu bị xung đột. Vui lòng thử lại."
                )
        );
    }
}
