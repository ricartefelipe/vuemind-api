package dev.vuemind.api.transfers.dto;

import java.time.Instant;

/** {@code scheduledFor} opcional: sem ele, o PIX é efetivado na hora. */
public record CreatePixRequest(String beneficiaryId, long amountCents, Instant scheduledFor) {
}
