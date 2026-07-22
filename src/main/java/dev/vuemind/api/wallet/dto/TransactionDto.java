package dev.vuemind.api.wallet.dto;

import dev.vuemind.api.store.model.TransactionType;
import java.time.Instant;

public record TransactionDto(
        String id,
        TransactionType type,
        long amountCents,
        String description,
        Instant createdAt,
        String counterparty) {
}
