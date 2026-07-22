package dev.vuemind.api.transfers.dto;

import java.time.Instant;

public record TransferDto(String id, String beneficiaryId, long amountCents, String status, Instant createdAt) {
}
