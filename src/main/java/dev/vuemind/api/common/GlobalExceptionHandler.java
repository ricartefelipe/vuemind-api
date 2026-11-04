package dev.vuemind.api.common;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApiException(ApiException ex, HttpServletRequest request) {
        ApiError body = new ApiError(ex.getCode(), ex.getMessage(), correlationIdOf(request));
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    /** Corpo malformado, como uma data de agendamento que não é ISO-8601, é erro do cliente, não do servidor. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        ApiError body = new ApiError(
                "VALIDATION_ERROR",
                "Corpo da requisição inválido. Datas seguem o formato ISO-8601, como 2026-11-04T15:30:00Z.",
                correlationIdOf(request));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        ApiError body = new ApiError(
                "INTERNAL_ERROR",
                "Erro inesperado ao processar a requisição.",
                correlationIdOf(request));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private String correlationIdOf(HttpServletRequest request) {
        String header = request.getHeader("X-Correlation-Id");
        return (header != null && !header.isBlank()) ? header : UUID.randomUUID().toString();
    }
}
