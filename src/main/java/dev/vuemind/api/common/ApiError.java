package dev.vuemind.api.common;

/**
 * Formato único de erro do contrato (mesmo shape do `ApiErrorBody` do
 * front Vue): `code` é a chave de negócio que a UI usa para decidir o que
 * fazer (ex.: "INSUFFICIENT_FUNDS" vira uma mensagem amigável específica),
 * `message` é o texto humano de fallback, e `correlationId` amarra essa
 * resposta ao log do servidor — em produção você grita o correlationId
 * pro time de suporte e acha a linha exata do erro nos logs.
 */
public record ApiError(String code, String message, String correlationId) {
}
