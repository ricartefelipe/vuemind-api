package dev.vuemind.api.transfers.dto;

public record CreatePixRequest(String beneficiaryId, long amountCents) {
}
