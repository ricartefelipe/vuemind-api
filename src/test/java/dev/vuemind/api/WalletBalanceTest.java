package dev.vuemind.api;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.vuemind.api.security.MockBearerTokenFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Cobre a rota protegida mais simples: sem Bearer válido, ninguém entra
 * (401, formato `ApiError`); com o token mock, o saldo semeado no
 * {@code InMemoryStore} (R$ 2.500,00) volta certinho.
 */
@SpringBootTest
@AutoConfigureMockMvc
class WalletBalanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void saldo_semTokenDeAutorizacao_devolve401() throws Exception {
        mockMvc.perform(get("/api/v1/wallet/balance"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
    }

    @Test
    void saldo_comTokenMockValido_devolveSaldoSemeadoEmBrl() throws Exception {
        mockMvc.perform(get("/api/v1/wallet/balance")
                        .header("Authorization", "Bearer " + MockBearerTokenFilter.MOCK_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableCents", is(250_000)))
                .andExpect(jsonPath("$.currency", is("BRL")));
    }
}
