package dev.vuemind.api.wallet;

import dev.vuemind.api.store.InMemoryStore;
import dev.vuemind.api.store.model.Transaction;
import dev.vuemind.api.store.model.TransactionType;
import dev.vuemind.api.wallet.dto.BalanceResponse;
import dev.vuemind.api.wallet.dto.TransactionDto;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class WalletService {

    private final InMemoryStore store;

    public WalletService(InMemoryStore store) {
        this.store = store;
    }

    public BalanceResponse getBalance() {
        return new BalanceResponse(store.availableCents().get(), "BRL");
    }

    public List<TransactionDto> getTransactions(Instant from, Instant to, String type) {
        TransactionType typeFilter = parseType(type);
        return store.transactions().stream()
                .filter(transaction -> from == null || !transaction.createdAt().isBefore(from))
                .filter(transaction -> to == null || !transaction.createdAt().isAfter(to))
                .filter(transaction -> typeFilter == null || transaction.type() == typeFilter)
                .sorted(Comparator.comparing(Transaction::createdAt).reversed())
                .map(this::toDto)
                .toList();
    }

    private TransactionType parseType(String type) {
        if (type == null || type.isBlank() || type.equalsIgnoreCase("ALL")) {
            return null;
        }
        return TransactionType.valueOf(type);
    }

    private TransactionDto toDto(Transaction transaction) {
        return new TransactionDto(
                transaction.id(),
                transaction.type(),
                transaction.amountCents(),
                transaction.description(),
                transaction.createdAt(),
                transaction.counterparty());
    }
}
