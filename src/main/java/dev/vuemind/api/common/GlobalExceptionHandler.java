package dev.vuemind.api.common;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Único ponto que converte exceções em respostas HTTP no formato do
 * contrato (`ApiError`). Assim os controllers ficam livres de try/catch:
 * cada service só lança `ApiException` (ou deixa vazar uma exceção
 * inesperada) e este handler cuida da tradução — o mesmo papel que o
 * `try/catch` dentro de cada handler MSW cumpre no front.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Erros de negócio esperados (credenciais inválidas, saldo insuficiente, etc.). */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApiException(ApiException ex, HttpServletRequest request) {
        ApiError body = new ApiError(ex.getCode(), ex.getMessage(), correlationIdOf(request));
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    /**
     * Rede de segurança para qualquer exceção não mapeada — nunca deveria
     * vazar stacktrace ou mensagem interna pro cliente HTTP.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        ApiError body = new ApiError(
                "INTERNAL_ERROR",
                "Erro inesperado ao processar a requisição.",
                correlationIdOf(request));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    /**
     * Reaproveita o `X-Correlation-Id` que o client já manda (ver
     * `shared/http/client.ts` no Vue); se não vier, gera um novo — igual ao
     * `createCorrelationId()` do mock.
     */
    private String correlationIdOf(HttpServletRequest request) {
        String header = request.getHeader("X-Correlation-Id");
        return (header != null && !header.isBlank()) ? header : UUID.randomUUID().toString();
    }
}
