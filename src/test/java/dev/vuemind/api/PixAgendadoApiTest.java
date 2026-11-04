package dev.vuemind.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import dev.vuemind.api.security.MockBearerTokenFilter;
import dev.vuemind.api.transfers.EndToEndIds;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** PIX agendado pela API, com o relógio real: o saldo dos outros testes não é afetado (contexto descartado no fim). */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PixAgendadoApiTest {

    private static final String BEARER = "Bearer " + MockBearerTokenFilter.MOCK_TOKEN;

    @Autowired
    private MockMvc mockMvc;

    private ResultActions agendar(String scheduledFor) throws Exception {
        return mockMvc.perform(post("/api/v1/transfers/pix")
                .header("Authorization", BEARER)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"beneficiaryId\":\"b1\",\"amountCents\":321,\"scheduledFor\":" + scheduledFor + "}"));
    }

    private long saldo() throws Exception {
        String body = mockMvc.perform(get("/api/v1/wallet/balance").header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.availableCents")).longValue();
    }

    @Test
    void agendadoNoFuturo_ficaPendenteEExecutaNaData() throws Exception {
        long antes = saldo();
        Instant quando = Instant.now().plusSeconds(2).truncatedTo(ChronoUnit.MILLIS);

        String criado = agendar("\"" + quando + "\"")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("SCHEDULED")))
                .andExpect(jsonPath("$.scheduledFor", is(quando.toString())))
                .andExpect(jsonPath("$.endToEndId", nullValue()))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(criado, "$.id");
        assertThat(saldo()).isEqualTo(antes);

        Thread.sleep(2_300);

        assertThat(saldo()).isEqualTo(antes - 321);
        String executado = mockMvc.perform(get("/api/v1/transfers/" + id).header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andReturn().getResponse().getContentAsString();
        assertThat((String) JsonPath.read(executado, "$.endToEndId")).matches(EndToEndIds.FORMATO);
    }

    @Test
    void dataNoPassado_responde400() throws Exception {
        agendar("\"" + Instant.now().minusSeconds(60) + "\"")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("SCHEDULE_IN_PAST")));
    }

    @Test
    void alemDe365Dias_responde400() throws Exception {
        agendar("\"" + Instant.now().plus(366, ChronoUnit.DAYS) + "\"")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("SCHEDULE_TOO_FAR")));
    }

    @Test
    void dataMalformada_responde400EmVezDe500() throws Exception {
        agendar("\"amanha\"")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
    }
}
