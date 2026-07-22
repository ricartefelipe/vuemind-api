package dev.vuemind.api.wallet.dto;

public record BalanceResponse(long availableCents, String currency) {
}
