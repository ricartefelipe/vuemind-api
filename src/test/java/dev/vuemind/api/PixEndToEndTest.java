package dev.vuemind.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import dev.vuemind.api.security.MockBearerTokenFilter;
import dev.vuemind.api.transfers.EndToEndIds;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class PixEndToEndTest {

    private static final String BEARER = "Bearer " + MockBearerTokenFilter.MOCK_TOKEN;

    @Autowired
    private MockMvc mockMvc;

    private ResultActions pix(String idempotencyKey, String correlationId) throws Exception {
        return mockMvc.perform(post("/api/v1/transfers/pix")
                .header("Authorization", BEARER)
                .header("Idempotency-Key", idempotencyKey)
                .header("X-Correlation-Id", correlationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"beneficiaryId":"b1","amountCents":150}
                        """));
    }

    @Test
    void pix_confirmado_devolveEndToEndNoFormatoPix_eACorrelacaoDaRequisicao() throws Exception {
        pix(UUID.randomUUID().toString(), "corr-pix-1")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andExpect(jsonPath("$.endToEndId", matchesPattern(EndToEndIds.FORMATO)))
                .andExpect(jsonPath("$.correlationId", is("corr-pix-1")));
    }

    @Test
    void pix_repetidoComAMesmaChave_naoGeraOutroEndToEnd() throws Exception {
        String key = UUID.randomUUID().toString();
        String primeiro = JsonPath.read(
                pix(key, "corr-a").andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(),
                "$.endToEndId");
        String segundo = JsonPath.read(
                pix(key, "corr-b").andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(),
                "$.endToEndId");

        assertThat(segundo).isEqualTo(primeiro);
    }

    @Test
    void pix_comChavesDiferentes_geraEndToEndsDiferentes() throws Exception {
        String primeiro = JsonPath.read(
                pix(UUID.randomUUID().toString(), "corr-c").andReturn().getResponse().getContentAsString(),
                "$.endToEndId");
        String segundo = JsonPath.read(
                pix(UUID.randomUUID().toString(), "corr-d").andReturn().getResponse().getContentAsString(),
                "$.endToEndId");

        assertThat(segundo).isNotEqualTo(primeiro);
    }

    @Test
    void consultaDaTransferencia_devolveOMesmoEndToEnd() throws Exception {
        String body = pix(UUID.randomUUID().toString(), "corr-e").andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.id");
        String endToEndId = JsonPath.read(body, "$.endToEndId");

        mockMvc.perform(get("/api/v1/transfers/" + id).header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(id)))
                .andExpect(jsonPath("$.endToEndId", is(endToEndId)))
                .andExpect(jsonPath("$.correlationId", is("corr-e")));
    }

    @Test
    void consultaDeTransferenciaInexistente_devolve404() throws Exception {
        mockMvc.perform(get("/api/v1/transfers/nao-existe").header("Authorization", BEARER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("TRANSFER_NOT_FOUND")));
    }

    @Test
    void pix_semCorrelacao_geraUmaParaOComprovante() throws Exception {
        mockMvc.perform(post("/api/v1/transfers/pix")
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"beneficiaryId":"b1","amountCents":100}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.correlationId", matchesPattern("[0-9a-f-]{36}")));
    }
}
