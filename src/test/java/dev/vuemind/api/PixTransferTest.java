package dev.vuemind.api;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.vuemind.api.security.MockBearerTokenFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PixTransferTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void pix_comValorMaiorQueSaldoDisponivel_devolve409InsufficientFunds() throws Exception {
        mockMvc.perform(post("/api/v1/transfers/pix")
                        .header("Authorization", "Bearer " + MockBearerTokenFilter.MOCK_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"beneficiaryId":"b1","amountCents":99999900}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("INSUFFICIENT_FUNDS")))
                .andExpect(jsonPath("$.correlationId").exists());
    }
}
