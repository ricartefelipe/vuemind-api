package dev.vuemind.api.wallet.reactive;

import dev.vuemind.api.wallet.WalletService;
import dev.vuemind.api.wallet.dto.BalanceResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Demo de endpoint reativo (Mono) coexistindo com o restante da API, que é
 * toda Spring MVC/blocking. Rota separada ({@code /api/v1/reactive/...}) para
 * não misturar contrato: o resto do app continua exatamente como está.
 *
 * <p><b>Blocking (MVC, resto da API) vs. non-blocking (WebFlux, aqui):</b> no
 * MVC clássico, cada requisição ocupa uma thread do pool do Tomcat do início
 * ao fim — se o service faz uma chamada de I/O (JDBC, HTTP para outro
 * serviço), a thread fica bloqueada esperando. Isso é simples e funciona bem
 * até o número de requisições concorrentes superar o tamanho do pool de
 * threads. No modelo reativo (WebFlux/Reactor), a requisição é modelada como
 * um pipeline assíncrono ({@code Mono}/{@code Flux}): a thread não fica
 * presa esperando I/O, ela é liberada e devolvida quando o resultado chega
 * (event loop), permitindo muito mais requisições concorrentes com menos
 * threads. O custo é a complexidade: debugar, empilhar side-effects
 * (map/flatMap) e garantir que NADA no pipeline bloqueie (ex.: trocar JDBC
 * por R2DBC) exige disciplina.
 *
 * <p><b>Nota importante desta demo:</b> como o app inteiro roda em Tomcat
 * (Servlet), este {@code Mono} é só o formato de retorno — o Spring MVC tem
 * suporte nativo a tipos reativos e resolve o {@code Mono} de forma
 * assíncrona sobre a mesma infraestrutura Servlet (não é Netty/event-loop de
 * ponta a ponta). Isso é honesto para o propósito da demo: mostrar a API de
 * programação reativa (Mono/operadores) e a integração sem quebrar nada do
 * MVC existente. Uma migração completa para non-blocking de verdade exigiria
 * trocar o {@code InMemoryStore}/JDBC por algo reativo (R2DBC) e rodar o app
 * inteiro em {@code spring.main.web-application-type=reactive} (Netty) — fora
 * do escopo aqui, ver README.
 *
 * <p><b>Quando usar WebFlux de verdade:</b> alta concorrência com I/O
 * predominantemente de rede (chamadas a outros serviços, bancos reativos),
 * poucos recursos por instância (containers pequenos), streaming
 * (Server-Sent Events, upload/download grandes). Para CRUD comum com baixa
 * concorrência, MVC blocking é mais simples de escrever, debugar e testar —
 * não é WebFlux por padrão, é WebFlux quando o gargalo é I/O concorrente.
 */
@RestController
@RequestMapping("/api/v1/reactive/wallet")
public class ReactiveWalletController {

    private final WalletService walletService;

    public ReactiveWalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    /**
     * Mesma leitura do {@code InMemoryStore} usada pelo endpoint blocking
     * ({@code WalletController#getBalance}), só embrulhada em {@code Mono}.
     * Mantém a mesma auth Bearer do resto da API — a rota cai na regra
     * {@code anyRequest().authenticated()} do {@code SecurityConfig}, não
     * precisou de nenhuma configuração extra.
     */
    @GetMapping("/balance")
    public Mono<BalanceResponse> getBalance() {
        return Mono.fromSupplier(walletService::getBalance);
    }
}
