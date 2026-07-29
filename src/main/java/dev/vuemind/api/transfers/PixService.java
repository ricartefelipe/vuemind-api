package dev.vuemind.api.transfers;

import dev.vuemind.api.common.ApiException;
import dev.vuemind.api.store.InMemoryStore;
import dev.vuemind.api.store.model.Beneficiary;
import dev.vuemind.api.store.model.Transaction;
import dev.vuemind.api.store.model.TransactionType;
import dev.vuemind.api.store.model.Transfer;
import dev.vuemind.api.transfers.dto.CreatePixRequest;
import dev.vuemind.api.transfers.dto.TransferDto;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class PixService {

    private final InMemoryStore store;

    public PixService(InMemoryStore store) {
        this.store = store;
    }

    public synchronized TransferDto executePix(CreatePixRequest request, String idempotencyKey) {
        Optional<Transfer> cached = Optional.ofNullable(store.idempotency().get(idempotencyKey));
        if (cached.isPresent()) {
            return toDto(cached.get());
        }

        Beneficiary beneficiary = store.beneficiaries().stream()
                .filter(item -> item.id().equals(request.beneficiaryId()))
                .findFirst()
                .orElseThrow(() -> new ApiException(
                        HttpStatus.BAD_REQUEST, "BENEFICIARY_NOT_FOUND", "Favorecido não encontrado."));

        if (request.amountCents() <= 0) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "INVALID_AMOUNT", "O valor da transferência deve ser positivo.");
        }

        if (store.availableCents().get() < request.amountCents()) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "INSUFFICIENT_FUNDS", "Saldo insuficiente para completar essa transferência.");
        }

        store.availableCents().addAndGet(-request.amountCents());

        Instant now = Instant.now();
        Transfer transfer = new Transfer(
                UUID.randomUUID().toString(), request.beneficiaryId(), request.amountCents(), "COMPLETED", now);
        store.transfers().add(transfer);
        store.transactions().add(new Transaction(
                UUID.randomUUID().toString(),
                TransactionType.PIX_OUT,
                request.amountCents(),
                "PIX para " + beneficiary.name(),
                now,
                beneficiary.name()));

        if (idempotencyKey != null) {
            store.idempotency().put(idempotencyKey, transfer);
        }

        return toDto(transfer);
    }

    private TransferDto toDto(Transfer transfer) {
        return new TransferDto(
                transfer.id(), transfer.beneficiaryId(), transfer.amountCents(), transfer.status(), transfer.createdAt());
    }
}
