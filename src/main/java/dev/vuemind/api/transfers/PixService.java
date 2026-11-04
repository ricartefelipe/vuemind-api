package dev.vuemind.api.transfers;

import dev.vuemind.api.common.ApiException;
import dev.vuemind.api.store.InMemoryStore;
import dev.vuemind.api.store.model.Beneficiary;
import dev.vuemind.api.store.model.Notification;
import dev.vuemind.api.store.model.OnboardingStepCode;
import dev.vuemind.api.store.model.Transaction;
import dev.vuemind.api.store.model.TransactionType;
import dev.vuemind.api.store.model.Transfer;
import dev.vuemind.api.transfers.dto.CreatePixRequest;
import dev.vuemind.api.transfers.dto.TransferDto;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * PIX simulado: debita o saldo da conta de demonstração, sem nenhuma chamada ao SPI.
 *
 * <p>O PIX agendado fica {@code SCHEDULED}, sem débito e sem end-to-end, até a data. Ele é executado na primeira
 * consulta depois dela (saldo, extrato, notificações ou a própria transferência), como no SpringMind: com saldo,
 * passa a {@code COMPLETED} e recebe o end-to-end nesse momento; sem saldo, passa a {@code FAILED} e gera uma
 * notificação.
 */
@Service
public class PixService {

    /** Até quando um PIX pode ser agendado. */
    public static final Duration MAX_SCHEDULE_AHEAD = Duration.ofDays(365);

    private final InMemoryStore store;
    private final Clock clock;

    public PixService(InMemoryStore store, Clock clock) {
        this.store = store;
        this.clock = clock;
    }

    public synchronized TransferDto executePix(CreatePixRequest request, String idempotencyKey, String correlationId) {
        String cachedId = idempotencyKey == null ? null : store.idempotency().get(idempotencyKey);
        if (cachedId != null) {
            processDueScheduledTransfers();
            return toDto(transferById(cachedId));
        }

        Beneficiary beneficiary = beneficiaryById(request.beneficiaryId());

        if (request.amountCents() <= 0) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "INVALID_AMOUNT", "O valor da transferência deve ser positivo.");
        }

        Instant now = clock.instant();
        Instant scheduledFor = request.scheduledFor();
        boolean isScheduled = scheduledFor != null;
        if (isScheduled) {
            validateSchedule(scheduledFor, now);
        } else if (store.availableCents().get() < request.amountCents()) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "INSUFFICIENT_FUNDS", "Saldo insuficiente para completar essa transferência.");
        }

        Transfer transfer = new Transfer(
                UUID.randomUUID().toString(),
                request.beneficiaryId(),
                request.amountCents(),
                isScheduled ? "SCHEDULED" : "COMPLETED",
                now,
                scheduledFor,
                // O end-to-end nasce quando o PIX é efetivado: o agendado recebe o seu na execução.
                isScheduled ? null : novoEndToEndId(now),
                correlationId);
        store.transfers().add(transfer);
        if (!isScheduled) {
            complete(transfer, beneficiary, now);
        }

        if (idempotencyKey != null) {
            store.idempotency().put(idempotencyKey, transfer.id());
        }

        return toDto(transfer);
    }

    public synchronized TransferDto findById(String id) {
        processDueScheduledTransfers();
        return toDto(transferById(id));
    }

    /** Executa os PIX agendados cuja data já chegou. Chamado antes de cada leitura que mostra saldo ou estado. */
    public synchronized void processDueScheduledTransfers() {
        Instant now = clock.instant();
        List<Transfer> transfers = store.transfers();
        for (int index = 0; index < transfers.size(); index++) {
            Transfer transfer = transfers.get(index);
            if (!"SCHEDULED".equals(transfer.status()) || transfer.scheduledFor().isAfter(now)) {
                continue;
            }
            if (store.availableCents().get() < transfer.amountCents()) {
                transfers.set(index, transfer.executed("FAILED", null));
                store.notifications().add(new Notification(
                        UUID.randomUUID().toString(),
                        "Transferência agendada falhou",
                        "Saldo insuficiente para concluir a transferência agendada.",
                        false,
                        now));
                continue;
            }
            Transfer executed = transfer.executed("COMPLETED", novoEndToEndId(now));
            transfers.set(index, executed);
            complete(executed, beneficiaryById(executed.beneficiaryId()), now);
        }
    }

    private void validateSchedule(Instant scheduledFor, Instant now) {
        if (!scheduledFor.isAfter(now)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "SCHEDULE_IN_PAST", "A data do agendamento precisa ser no futuro.");
        }
        if (scheduledFor.isAfter(now.plus(MAX_SCHEDULE_AHEAD))) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "SCHEDULE_TOO_FAR",
                    "O PIX pode ser agendado para até 365 dias a partir de agora.");
        }
    }

    private void complete(Transfer transfer, Beneficiary beneficiary, Instant now) {
        store.availableCents().addAndGet(-transfer.amountCents());
        store.transactions().add(new Transaction(
                UUID.randomUUID().toString(),
                TransactionType.PIX_OUT,
                transfer.amountCents(),
                "PIX para " + beneficiary.name(),
                now,
                beneficiary.name()));
        store.completedSteps().add(OnboardingStepCode.FIRST_PIX);
        store.notifications().add(new Notification(
                UUID.randomUUID().toString(),
                "PIX enviado",
                "Transferência de " + transfer.amountCents() + " centavos para " + beneficiary.name() + " foi concluída.",
                false,
                now));
    }

    private Beneficiary beneficiaryById(String id) {
        return store.beneficiaries().stream()
                .filter(item -> item.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new ApiException(
                        HttpStatus.BAD_REQUEST, "BENEFICIARY_NOT_FOUND", "Favorecido não encontrado."));
    }

    private Transfer transferById(String id) {
        return store.transfers().stream()
                .filter(item -> item.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "TRANSFER_NOT_FOUND", "Transferência não encontrada."));
    }

    /** Gera o identificador na efetivação; repete o sorteio no caso, improvável, de colidir com um já emitido. */
    private String novoEndToEndId(Instant momento) {
        String id;
        do {
            id = EndToEndIds.gerar(momento);
        } while (store.endToEndIds().contains(id));
        store.endToEndIds().add(id);
        return id;
    }

    private TransferDto toDto(Transfer transfer) {
        return new TransferDto(
                transfer.id(),
                transfer.beneficiaryId(),
                transfer.amountCents(),
                transfer.status(),
                transfer.createdAt(),
                transfer.scheduledFor(),
                transfer.endToEndId(),
                transfer.correlationId());
    }
}
