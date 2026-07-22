package dev.vuemind.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * Ponto de entrada. Skeleton de estudo: o objetivo aqui não é "produto
 * completo", é mostrar a espinha dorsal de uma API Spring bem organizada —
 * camadas claras (controller → service → store), erros padronizados e
 * segurança mínima — para depois evoluir com JPA, mensageria e WebFlux sem
 * reescrever nada do que já existe.
 *
 * Excluímos o autoconfig de usuário padrão do Spring Security: como
 * autenticamos via {@code MockBearerTokenFilter} (não via login
 * form/basic), o `UserDetailsService` gerado automaticamente (com senha
 * aleatória no log a cada start) não tem função aqui.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class VuemindApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(VuemindApiApplication.class, args);
    }
}
