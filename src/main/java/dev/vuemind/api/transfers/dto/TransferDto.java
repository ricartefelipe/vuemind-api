package dev.vuemind.api.transfers.dto;

import java.time.Instant;

/**
 * Transferência PIX simulada. {@code endToEndId} segue o formato do arranjo PIX, com ISPB fictício, e só existe depois
 * da efetivação: o agendado ({@code SCHEDULED}) recebe o seu quando é executado.
 */
public record TransferDto(
        String id,
        String beneficiaryId,
        long amountCents,
        String status,
        Instant createdAt,
        Instant scheduledFor,
        String endToEndId,
        String correlationId) {
}
