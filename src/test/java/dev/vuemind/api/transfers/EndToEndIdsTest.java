package dev.vuemind.api.transfers;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

class EndToEndIdsTest {

    @Test
    void gerar_segueFormatoDoArranjoPix_comIspbFicticioEDataHoraUtc() {
        String id = EndToEndIds.gerar(Instant.parse("2026-11-03T23:59:41Z"));

        assertThat(id).hasSize(32).matches(EndToEndIds.FORMATO);
        assertThat(id).startsWith("E" + EndToEndIds.ISPB_DEMONSTRACAO + "202611032359");
    }

    @Test
    void gerar_usaUtcMesmoPertoDaVirada_doDia() {
        assertThat(EndToEndIds.gerar(Instant.parse("2026-12-31T23:59:59Z"))).startsWith("E99999999202612312359");
        assertThat(EndToEndIds.gerar(Instant.parse("2027-01-01T00:00:00Z"))).startsWith("E99999999202701010000");
    }

    @Test
    void gerar_naoRepete_emMilharesDeChamadasNoMesmoMinuto() {
        Instant momento = Instant.parse("2026-11-03T12:00:00Z");
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < 20_000; i++) {
            ids.add(EndToEndIds.gerar(momento));
        }
        assertThat(ids).hasSize(20_000).allMatch(id -> EndToEndIds.FORMATO.matcher(id).matches());
    }

    @Test
    void gerar_comMesmaSemente_eDeterministico() {
        Instant momento = Instant.parse("2026-11-03T12:00:00Z");
        assertThat(EndToEndIds.gerar(momento, new SplittableRandom(7)))
                .isEqualTo(EndToEndIds.gerar(momento, new SplittableRandom(7)));
    }

    @Test
    void formato_recusaIdentificadoresForaDoPadrao() {
        assertThat(EndToEndIds.FORMATO.matcher("E" + "0".repeat(32)).matches()).isFalse();
        assertThat(EndToEndIds.FORMATO.matcher("E99999999202613032359abcdefghijk").matches()).isFalse();
        assertThat(EndToEndIds.FORMATO.matcher("X99999999202611032359abcdefghijk").matches()).isFalse();
        assertThat(EndToEndIds.FORMATO.matcher("E99999999202611032359abcdefghij-").matches()).isFalse();
    }
}
