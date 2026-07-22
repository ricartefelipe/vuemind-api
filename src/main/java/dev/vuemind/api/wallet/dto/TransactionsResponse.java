package dev.vuemind.api.wallet.dto;

import java.util.List;

public record TransactionsResponse(List<TransactionDto> items) {
}
