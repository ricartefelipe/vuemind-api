package dev.vuemind.api;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.vuemind.api.security.MockBearerTokenFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BeneficiariesTest {

    private static final String BEARER = "Bearer " + MockBearerTokenFilter.MOCK_TOKEN;

    @Autowired
    private MockMvc mockMvc;

    private ResultActions criar(String json) throws Exception {
        return mockMvc.perform(post("/api/v1/beneficiaries")
                .header("Authorization", BEARER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    @Test
    void lista_trazOTipoDaChave() throws Exception {
        mockMvc.perform(get("/api/v1/beneficiaries").header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id == 'b1')].pixKeyType", hasItem("EMAIL")))
                .andExpect(jsonPath("$.items[?(@.id == 'b2')].pixKeyType", hasItem("CNPJ")))
                .andExpect(jsonPath("$.items[?(@.id == 'b2')].pixKey", hasItem("11222333000181")));
    }

    @Test
    void criar_guardaEDevolveOTipo() throws Exception {
        criar("""
                {"name":"Bruno","pixKey":"+5511987654321","pixKeyType":"PHONE"}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pixKeyType", is("PHONE")))
                .andExpect(jsonPath("$.pixKey", is("+5511987654321")));
    }

    @Test
    void chaveQueNaoCombinaComOTipo_responde400() throws Exception {
        criar("""
                {"name":"Bruno","pixKey":"nao-e-email","pixKeyType":"EMAIL"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_PIX_KEY")));
    }

    @Test
    void semTipo_responde400() throws Exception {
        criar("""
                {"name":"Bruno","pixKey":"bruno@email.com"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_BENEFICIARY")));
    }

    @Test
    void tipoDesconhecido_responde400() throws Exception {
        criar("""
                {"name":"Bruno","pixKey":"11222333000181","pixKeyType":"PASSAPORTE"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
    }

    @Test
    void cnpj_validoEntraNumericoEAlfanumerico() throws Exception {
        criar("""
                {"name":"Padaria","pixKey":"11222333000181","pixKeyType":"CNPJ"}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pixKeyType", is("CNPJ")));
        criar("""
                {"name":"Padaria Nova","pixKey":"12ABC34501DE35","pixKeyType":"CNPJ"}
                """)
                .andExpect(status().isCreated());
    }

    @Test
    void digitoVerificadorErrado_responde400() throws Exception {
        criar("""
                {"name":"Padaria","pixKey":"11222333000182","pixKeyType":"CNPJ"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_PIX_KEY")));
        criar("""
                {"name":"Bruno","pixKey":"12345678901","pixKeyType":"CPF"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_PIX_KEY")));
    }
}
