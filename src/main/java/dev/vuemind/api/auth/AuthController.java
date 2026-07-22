package dev.vuemind.api.auth;

import dev.vuemind.api.auth.dto.LoginRequest;
import dev.vuemind.api.auth.dto.LoginResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Única rota pública da API (ver {@code SecurityConfig}). Devolve um token
 * opaco fixo — o mesmo truque do mock MSW — porque o objetivo deste
 * skeleton é mostrar a forma da API, não uma implementação de JWT completa.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
