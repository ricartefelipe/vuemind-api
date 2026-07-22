package dev.vuemind.api.store.model;

import java.time.Instant;

public record Transaction(
        String id,
        TransactionType type,
        long amountCents,
        String description,
        Instant createdAt,
        String counterparty) {
}
