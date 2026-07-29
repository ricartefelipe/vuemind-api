package dev.vuemind.api;

import dev.vuemind.api.security.MockBearerTokenFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class ReactiveWalletBalanceTest {

    @Autowired
    private WebTestClient client;

    @Test
    void saldoReativo_semTokenDeAutorizacao_devolve401() {
        client.get()
                .uri("/api/v1/reactive/wallet/balance")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.code").isEqualTo("UNAUTHORIZED");
    }

    @Test
    void saldoReativo_comTokenMockValido_devolveSaldoSemeadoEmBrl() {
        client.get()
                .uri("/api/v1/reactive/wallet/balance")
                .header("Authorization", "Bearer " + MockBearerTokenFilter.MOCK_TOKEN)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.availableCents").isEqualTo(250_000)
                .jsonPath("$.currency").isEqualTo("BRL");
    }
}
