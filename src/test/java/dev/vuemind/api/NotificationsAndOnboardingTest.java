package dev.vuemind.api;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.vuemind.api.security.MockBearerTokenFilter;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class NotificationsAndOnboardingTest {

    private static final String BEARER = "Bearer " + MockBearerTokenFilter.MOCK_TOKEN;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void notificacoes_semToken_devolve401() throws Exception {
        mockMvc.perform(get("/api/v1/notifications")).andExpect(status().isUnauthorized());
    }

    @Test
    void notificacoes_listaSemeadaDaMaisRecenteParaMaisAntiga() throws Exception {
        mockMvc.perform(get("/api/v1/notifications").header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()", is(3)))
                .andExpect(jsonPath("$.items[0].id", is("n3")))
                .andExpect(jsonPath("$.items[0].read", is(false)));
    }

    @Test
    void notificacoes_marcaUmaComoLida() throws Exception {
        mockMvc.perform(post("/api/v1/notifications/n1/read").header("Authorization", BEARER))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/notifications").header("Authorization", BEARER))
                .andExpect(jsonPath("$.items[2].id", is("n1")))
                .andExpect(jsonPath("$.items[2].read", is(true)))
                .andExpect(jsonPath("$.items[0].read", is(false)));
    }

    @Test
    void notificacoes_inexistente_devolve404() throws Exception {
        mockMvc.perform(post("/api/v1/notifications/nao-existe/read").header("Authorization", BEARER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("NOTIFICATION_NOT_FOUND")));
    }

    @Test
    void notificacoes_marcaTodasComoLidas() throws Exception {
        mockMvc.perform(post("/api/v1/notifications/read-all").header("Authorization", BEARER))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/notifications").header("Authorization", BEARER))
                .andExpect(jsonPath("$.items[*].read", org.hamcrest.Matchers.everyItem(is(true))));
    }

    @Test
    void onboarding_estadoInicial() throws Exception {
        mockMvc.perform(get("/api/v1/me/onboarding").header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed", is(false)))
                .andExpect(jsonPath("$.steps.length()", is(4)))
                .andExpect(jsonPath("$.steps[?(@.id=='PROFILE_OK')].done", hasItem(true)))
                .andExpect(jsonPath("$.steps[?(@.id=='FIRST_PIX')].done", hasItem(false)))
                .andExpect(jsonPath("$.steps[?(@.id=='VIEW_STATEMENT')].done", hasItem(false)));
    }

    @Test
    void pixEExtrato_concluemOnboardingEGeramNotificacao() throws Exception {
        mockMvc.perform(post("/api/v1/transfers/pix")
                        .header("Authorization", BEARER)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"beneficiaryId\":\"b1\",\"amountCents\":1000}"))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/v1/wallet/transactions").header("Authorization", BEARER))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/me/onboarding").header("Authorization", BEARER))
                .andExpect(jsonPath("$.completed", is(true)));
        mockMvc.perform(get("/api/v1/notifications").header("Authorization", BEARER))
                .andExpect(jsonPath("$.items.length()", is(4)))
                .andExpect(jsonPath("$.items[0].title", is("PIX enviado")));
    }
}
