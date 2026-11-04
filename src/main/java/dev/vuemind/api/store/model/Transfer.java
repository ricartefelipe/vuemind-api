package dev.vuemind.api.store.model;

import java.time.Instant;

/** {@code scheduledFor} vem só no PIX agendado; {@code endToEndId} só existe depois da efetivação. */
public record Transfer(
        String id,
        String beneficiaryId,
        long amountCents,
        String status,
        Instant createdAt,
        Instant scheduledFor,
        String endToEndId,
        String correlationId) {

    public Transfer executed(String status, String endToEndId) {
        return new Transfer(id, beneficiaryId, amountCents, status, createdAt, scheduledFor, endToEndId, correlationId);
    }
}
