package dev.vuemind.api.common;

public record ApiError(String code, String message, String correlationId) {
}
