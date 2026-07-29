package dev.vuemind.api;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void login_comCredenciaisValidas_devolveTokenMockETodosOsDadosDoUsuario() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"demo@vuemind.dev","password":"demo123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", is("mock-jwt-demo")))
                .andExpect(jsonPath("$.user.email", is("demo@vuemind.dev")));
    }

    @Test
    void login_comSenhaErrada_devolve401NoFormatoApiError() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"demo@vuemind.dev","password":"senha-errada"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("INVALID_CREDENTIALS")))
                .andExpect(jsonPath("$.correlationId").exists());
    }
}
