package dev.vuemind.api.store.model;

import java.time.Instant;

public record Transfer(
        String id,
        String beneficiaryId,
        long amountCents,
        String status,
        Instant createdAt) {
}
