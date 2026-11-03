package dev.vuemind.api.transfers.dto;

import java.time.Instant;

/** Transferência PIX simulada. {@code endToEndId} segue o formato do arranjo PIX, com ISPB fictício. */
public record TransferDto(
        String id,
        String beneficiaryId,
        long amountCents,
        String status,
        Instant createdAt,
        String endToEndId,
        String correlationId) {
}
