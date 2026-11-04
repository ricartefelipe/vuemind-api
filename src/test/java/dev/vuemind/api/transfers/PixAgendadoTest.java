package dev.vuemind.api.transfers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.vuemind.api.common.ApiException;
import dev.vuemind.api.store.InMemoryStore;
import dev.vuemind.api.store.model.Beneficiary;
import dev.vuemind.api.store.model.PixKeyType;
import dev.vuemind.api.transfers.dto.CreatePixRequest;
import dev.vuemind.api.transfers.dto.TransferDto;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PixAgendadoTest {

    /** Relógio que o teste avança, para ver o agendado ser executado na hora certa. */
    static final class RelogioControlado extends Clock {
        private Instant agora;

        RelogioControlado(Instant inicio) {
            this.agora = inicio;
        }

        void avancar(Duration duracao) {
            agora = agora.plus(duracao);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return agora;
        }
    }

    private static final Instant INICIO = Instant.parse("2026-11-04T12:00:00Z");

    private InMemoryStore store;
    private RelogioControlado relogio;
    private PixService pix;

    @BeforeEach
    void preparar() {
        store = new InMemoryStore();
        store.beneficiaries().add(new Beneficiary("b1", "Ana Silva", "ana@email.com", PixKeyType.EMAIL));
        store.availableCents().set(10_000);
        relogio = new RelogioControlado(INICIO);
        pix = new PixService(store, relogio);
    }

    private TransferDto agendar(long centavos, Instant quando, String chave) {
        return pix.executePix(new CreatePixRequest("b1", centavos, quando), chave, "corr-" + chave);
    }

    @Test
    void agendado_ficaPendenteSemDebitoESemEndToEnd() {
        TransferDto criado = agendar(2_500, INICIO.plus(Duration.ofMinutes(5)), "k1");

        assertThat(criado.status()).isEqualTo("SCHEDULED");
        assertThat(criado.scheduledFor()).isEqualTo(INICIO.plus(Duration.ofMinutes(5)));
        assertThat(criado.endToEndId()).isNull();
        assertThat(store.availableCents().get()).isEqualTo(10_000);
        assertThat(store.transactions()).isEmpty();
    }

    @Test
    void antesDaData_continuaPendente() {
        TransferDto criado = agendar(2_500, INICIO.plus(Duration.ofMinutes(5)), "k1");
        relogio.avancar(Duration.ofMinutes(4).plusSeconds(59));

        TransferDto consultado = pix.findById(criado.id());

        assertThat(consultado.status()).isEqualTo("SCHEDULED");
        assertThat(consultado.endToEndId()).isNull();
        assertThat(store.availableCents().get()).isEqualTo(10_000);
    }

    @Test
    void naData_executaDebitaEGeraOEndToEndDaExecucao() {
        TransferDto criado = agendar(2_500, INICIO.plus(Duration.ofMinutes(5)), "k1");
        relogio.avancar(Duration.ofMinutes(7));

        TransferDto executado = pix.findById(criado.id());

        assertThat(executado.status()).isEqualTo("COMPLETED");
        assertThat(executado.endToEndId()).matches(EndToEndIds.FORMATO);
        // O carimbo do end-to-end é o da execução (12:07), não o da criação (12:00) nem o agendado (12:05).
        assertThat(executado.endToEndId().substring(9, 21)).isEqualTo("202611041207");
        assertThat(store.availableCents().get()).isEqualTo(7_500);
        assertThat(store.transactions()).hasSize(1);
        assertThat(store.notifications()).anyMatch(n -> n.title().equals("PIX enviado"));
    }

    @Test
    void executaUmaVezSo() {
        TransferDto criado = agendar(2_500, INICIO.plus(Duration.ofMinutes(5)), "k1");
        relogio.avancar(Duration.ofMinutes(6));

        String primeiro = pix.findById(criado.id()).endToEndId();
        pix.processDueScheduledTransfers();
        relogio.avancar(Duration.ofHours(1));
        String segundo = pix.findById(criado.id()).endToEndId();

        assertThat(segundo).isEqualTo(primeiro);
        assertThat(store.availableCents().get()).isEqualTo(7_500);
        assertThat(store.transactions()).hasSize(1);
    }

    @Test
    void repeticaoComAMesmaChave_devolveOEstadoAtual() {
        Instant quando = INICIO.plus(Duration.ofMinutes(5));
        TransferDto criado = agendar(2_500, quando, "k1");
        relogio.avancar(Duration.ofMinutes(6));

        TransferDto repetido = agendar(2_500, quando, "k1");

        assertThat(repetido.id()).isEqualTo(criado.id());
        assertThat(repetido.status()).isEqualTo("COMPLETED");
        assertThat(repetido.endToEndId()).matches(EndToEndIds.FORMATO);
        assertThat(store.transfers()).hasSize(1);
    }

    @Test
    void semSaldoNaData_falhaENotifica() {
        TransferDto criado = agendar(20_000, INICIO.plus(Duration.ofMinutes(5)), "k1");
        relogio.avancar(Duration.ofMinutes(6));

        TransferDto falhou = pix.findById(criado.id());

        assertThat(falhou.status()).isEqualTo("FAILED");
        assertThat(falhou.endToEndId()).isNull();
        assertThat(store.availableCents().get()).isEqualTo(10_000);
        assertThat(store.notifications()).anyMatch(n -> n.title().equals("Transferência agendada falhou"));
    }

    @Test
    void dataNoPassadoOuAgora_recusa() {
        assertThatThrownBy(() -> agendar(100, INICIO.minusSeconds(1), "k1"))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("SCHEDULE_IN_PAST");
        assertThatThrownBy(() -> agendar(100, INICIO, "k2"))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("SCHEDULE_IN_PAST");
        assertThat(store.transfers()).isEmpty();
    }

    @Test
    void alemDe365Dias_recusa() {
        assertThat(agendar(100, INICIO.plus(PixService.MAX_SCHEDULE_AHEAD), "k1").status()).isEqualTo("SCHEDULED");
        assertThatThrownBy(() -> agendar(100, INICIO.plus(PixService.MAX_SCHEDULE_AHEAD).plusSeconds(1), "k2"))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("SCHEDULE_TOO_FAR");
    }

    @Test
    void semData_continuaImediato() {
        TransferDto imediato = pix.executePix(new CreatePixRequest("b1", 1_000, null), "k1", "corr");

        assertThat(imediato.status()).isEqualTo("COMPLETED");
        assertThat(imediato.scheduledFor()).isNull();
        assertThat(imediato.endToEndId()).matches(EndToEndIds.FORMATO);
        assertThat(store.availableCents().get()).isEqualTo(9_000);
    }
}
