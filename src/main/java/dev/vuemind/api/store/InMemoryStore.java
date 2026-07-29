package dev.vuemind.api.store;

import dev.vuemind.api.store.model.Beneficiary;
import dev.vuemind.api.store.model.MockUser;
import dev.vuemind.api.store.model.Transaction;
import dev.vuemind.api.store.model.TransactionType;
import dev.vuemind.api.store.model.Transfer;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
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
    private final Map<String, Transfer> idempotency = new ConcurrentHashMap<>();

    @PostConstruct
    void seed() {
        beneficiaries.add(new Beneficiary("b1", "Ana Silva", "ana@email.com"));
        beneficiaries.add(new Beneficiary("b2", "Mercado Central", "11222333000181"));

        transactions.add(new Transaction(
                "t1",
                TransactionType.PIX_IN,
                50_000,
                "Recebido",
                Instant.now().minus(2, ChronoUnit.DAYS),
                "Carlos"));
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

    public Map<String, Transfer> idempotency() {
        return idempotency;
    }
}
