package dev.vuemind.api.common;

import org.springframework.http.HttpStatus;

/**
 * Exceção de negócio única para toda a API. Em vez de criar uma classe por
 * erro (InsufficientFundsException, BeneficiaryNotFoundException, ...), cada
 * regra apenas lança `new ApiException(status, "CODE", "mensagem")` — o
 * `GlobalExceptionHandler` traduz isso pro JSON do contrato. Simples de
 * propósito: para um catálogo de erros maior, vale migrar para um enum de
 * códigos de negócio, mas aqui o mapa mental cabe na cabeça em 2 minutos.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
