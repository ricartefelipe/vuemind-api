package dev.vuemind.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Segurança propositalmente simples: o contrato (`vuemind-wallet-openapi.yaml`)
 * já documenta que o token é "opaco mock (mock-jwt-demo); no Spring real será
 * um JWT". Este filtro só confere se o header `Authorization` é exatamente
 * `Bearer mock-jwt-demo` — o mesmo token fixo que `AuthService` devolve no
 * login e que o MSW do front usa hoje.
 *
 * Isso NÃO é o desenho final: não expira, não carrega claims, não distingue
 * usuários. É o suficiente para exercitar "rota pública vs. protegida" e
 * "401 sem token". Trocar por JWT real (Nimbus/JJWT com claims de usuário,
 * expiração e assinatura) é o próximo passo natural, sem mudar o resto da
 * cadeia (controllers continuam pedindo `@AuthenticationPrincipal` ou nada).
 */
public class MockBearerTokenFilter extends OncePerRequestFilter {

    public static final String MOCK_TOKEN = "mock-jwt-demo";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.equals(BEARER_PREFIX + MOCK_TOKEN)) {
            var authentication = new UsernamePasswordAuthenticationToken(
                    "demo-user", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        // Sem token válido: não autentica aqui. Quem decide se a rota exige
        // autenticação é o `SecurityConfig` — este filtro só tenta autenticar.
        filterChain.doFilter(request, response);
    }
}
