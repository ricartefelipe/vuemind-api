package dev.vuemind.api.store;

import dev.vuemind.api.store.model.Beneficiary;
import dev.vuemind.api.store.model.MockUser;
import dev.vuemind.api.store.model.Notification;
import dev.vuemind.api.store.model.OnboardingStepCode;
import dev.vuemind.api.store.model.PixKeyType;
import dev.vuemind.api.store.model.Transaction;
import dev.vuemind.api.store.model.TransactionType;
import dev.vuemind.api.store.model.Transfer;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class InMemoryStore {

    private final MockUser user = new MockUser("u1", "Felipe Demo", "demo@vuemind.dev", "demo123");

    private final AtomicLong availableCents = new AtomicLong(250_000);
    private final List<Beneficiary> beneficiaries = new CopyOnWriteArrayList<>();
    private final List<Transaction> transactions = new CopyOnWriteArrayList<>();
    private final List<Transfer> transfers = new CopyOnWriteArrayList<>();
    private final Map<String, String> idempotency = new ConcurrentHashMap<>();
    private final Set<String> endToEndIds = ConcurrentHashMap.newKeySet();
    private final List<Notification> notifications = new CopyOnWriteArrayList<>();
    private final Set<OnboardingStepCode> completedSteps = Collections.synchronizedSet(EnumSet.noneOf(OnboardingStepCode.class));

    @PostConstruct
    void seed() {
        // Os mesmos favorecidos dos dados simulados do front (mind-shared), com o tipo da chave.
        beneficiaries.add(new Beneficiary("b1", "Ana Silva", "ana@email.com", PixKeyType.EMAIL));
        beneficiaries.add(new Beneficiary("b2", "Mercado Central", "11222333000", PixKeyType.CPF));

        transactions.add(new Transaction(
                "t1",
                TransactionType.PIX_IN,
                50_000,
                "Recebido",
                Instant.now().minus(2, ChronoUnit.DAYS),
                "Carlos"));

        Instant now = Instant.now();
        notifications.add(new Notification(
                "n1", "PIX recebido", "Você recebeu um PIX de R$ 500,00 de Carlos.", false, now.minus(2, ChronoUnit.DAYS)));
        notifications.add(new Notification(
                "n2",
                "Limite diário próximo",
                "Você já utilizou uma parte relevante do seu limite diário de PIX.",
                false,
                now.minus(1, ChronoUnit.DAYS)));
        notifications.add(new Notification(
                "n3",
                "Complete seu onboarding",
                "Cadastre um favorecido e faça seu primeiro PIX para liberar todo o app.",
                false,
                now.minus(1, ChronoUnit.HOURS)));

        completedSteps.add(OnboardingStepCode.PROFILE_OK);
        completedSteps.add(OnboardingStepCode.FIRST_BENEFICIARY);
    }

    public MockUser user() {
        return user;
    }

    public AtomicLong availableCents() {
        return availableCents;
    }

    public List<Beneficiary> beneficiaries() {
        return beneficiaries;
    }

    public List<Transaction> transactions() {
        return transactions;
    }

    public List<Transfer> transfers() {
        return transfers;
    }

    /** Chave de idempotência para o id da transferência: a repetição devolve o estado atual dela. */
    public Map<String, String> idempotency() {
        return idempotency;
    }

    /** Identificadores end-to-end já emitidos, para garantir que nenhum se repita. */
    public Set<String> endToEndIds() {
        return endToEndIds;
    }

    public List<Notification> notifications() {
        return notifications;
    }

    public Set<OnboardingStepCode> completedSteps() {
        return completedSteps;
    }
}
