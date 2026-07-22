# VueMind API

Backend Spring Boot da carteira digital de estudo **VueMind Wallet**. Implementa
o contrato em `docs/contracts/vuemind-wallet-openapi.yaml` (repositório
`vuemind`) com dados em memória — skeleton propositalmente mínimo, sem
banco/mensageria/cloud ainda.

## Stack

- Java 21 + Spring Boot 3.3 (Web, Security, WebFlux só para o endpoint reativo de demo)
- Sem banco: `InMemoryStore` (listas + `AtomicLong` + `ConcurrentHashMap`)
- Sem JWT real: token opaco fixo (`mock-jwt-demo`), igual ao mock MSW do front
- Testes: MockMvc (`spring-boot-starter-test`) + `WebTestClient` para o endpoint reativo

## Como rodar

```bash
./mvnw spring-boot:run
```

Sobe em `http://localhost:8080`. Rodar os testes:

```bash
./mvnw test
```

> Se a porta 8080 já estiver ocupada na sua máquina, suba em outra porta com
> `./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8082`.

## Login de demonstração

```
email: demo@vuemind.dev
senha: demo123
```

## Como apontar o Vue para esta API

O client HTTP do Vue (`src/shared/http/client.ts`) chama sempre um caminho
relativo `/api/v1/...`; hoje o MSW intercepta isso em dev. Para testar contra
esta API real, duas opções simples (nenhuma delas está aplicada no repo do
Vue — são passos manuais para a demo):

**Opção A — proxy do Vite (recomendada, não mexe no client HTTP):**
adicionar em `vite.config.ts` do projeto Vue:

```ts
server: {
  proxy: {
    '/api/v1': 'http://localhost:8080',
  },
},
```

e comentar a chamada `await worker.start(...)` em `src/main.ts` (ou simplesmente
não rodar em modo `DEV` com MSW ativo).

**Opção B — desligar o MSW e usar CORS direto:** comentar o bloco
`if (import.meta.env.DEV) { ... worker.start ... }` em `main.ts` e trocar a
constante `BASE` de `shared/http/client.ts` para uma URL absoluta
(`http://localhost:8080/api/v1`, futuramente via `VITE_API_BASE_URL`). O CORS
desta API já libera qualquer origem `http://localhost:*`.

## Endpoints implementados

| Método | Rota | Auth |
|---|---|---|
| POST | `/api/v1/auth/login` | pública |
| GET | `/api/v1/wallet/balance` | Bearer |
| GET | `/api/v1/wallet/transactions?from&to&type` | Bearer |
| GET | `/api/v1/beneficiaries` | Bearer |
| POST | `/api/v1/beneficiaries` | Bearer |
| DELETE | `/api/v1/beneficiaries/{id}` | Bearer |
| POST | `/api/v1/transfers/pix` (aceita header `Idempotency-Key`) | Bearer |
| GET | `/api/v1/reactive/wallet/balance` (demo WebFlux, `Mono<BalanceResponse>`) | Bearer |

Autenticação: enviar `Authorization: Bearer mock-jwt-demo` (token fixo
devolvido pelo login).

## Endpoint reativo de demo (WebFlux)

```bash
curl -H "Authorization: Bearer mock-jwt-demo" http://localhost:8080/api/v1/reactive/wallet/balance
```

Resposta (mesmo formato do endpoint blocking, mesmo saldo semeado):

```json
{"availableCents":250000,"currency":"BRL"}
```

**Onde está:** `wallet/reactive/ReactiveWalletController.java` — reusa o
`WalletService` (mesma leitura do `InMemoryStore`), só embrulhada em
`Mono.fromSupplier(...)`. Comentário no controller explica blocking vs.
non-blocking e quando WebFlux vale a pena.

**Decisão de arquitetura (MVC + WebFlux no mesmo app):** adicionei a
dependência `spring-boot-starter-webflux` ao lado do `spring-boot-starter-web`
já existente — **Opção A** do problema (controller reativo no mesmo app, path
separado), não perfil/módulo separado. Funciona sem conflito de porta/servidor
porque o Spring Boot decide o tipo de web app pelo classpath: havendo Servlet
(Tomcat) presente, ele nunca promove a aplicação para `WebApplicationType.REACTIVE`
mesmo com Reactor no classpath — o app inteiro continua em Tomcat/MVC. O
`Mono<BalanceResponse>` retornado pelo controller é resolvido pelo suporte
nativo do Spring MVC a tipos reativos (via `DeferredResult`/async request),
**não** por um event-loop Netty de ponta a ponta. Para a demo isso é
suficiente e honesto: mostra a API de programação reativa sem reescrever a
stack.

**Pegadinha real que apareceu e foi corrigida:** o retorno `Mono` faz o Spring
MVC processar a requisição em duas passagens pela filter chain (dispatch
inicial + dispatch assíncrono quando o `Mono` completa). O
`MockBearerTokenFilter`, por ser um `OncePerRequestFilter`, por padrão **não**
roda de novo no dispatch assíncrono — como a API é stateless (sem sessão), o
`SecurityContext` autenticado no primeiro dispatch nunca é persistido, e a
segunda passagem chegava ao `AuthorizationFilter` sem autenticação → 401
mesmo com token válido. Fix de uma linha: `shouldNotFilterAsyncDispatch()`
retornando `false` nesse filtro, para reautenticar (é só reler o header) em
cada passagem. Só afeta esse filtro específico, sem tocar na estratégia global
de `SecurityContext`.

**Se o objetivo fosse non-blocking de ponta a ponta** (não é o caso aqui): trocar
`InMemoryStore`/JDBC por R2DBC, subir o app com
`spring.main.web-application-type=reactive` (Netty) e remover o
`spring-boot-starter-web` — mudança maior, fora do escopo desta demo.

## Estrutura

```
common/        ApiError, ApiException, GlobalExceptionHandler (erros padronizados)
security/      filtro de Bearer mock + SecurityConfig + CORS
store/         "banco" em memória (InMemoryStore) + modelos
auth/          login
wallet/        saldo e extrato
beneficiaries/ CRUD de favorecidos
transfers/     PIX (regra de negócio + idempotência)
```

## O que dizer na entrevista (60s)

- Camadas claras: Controller → Service → Store, sem lógica de negócio no
  controller.
- Erro único (`ApiError`) via `@RestControllerAdvice` — mesmo contrato que o
  mock MSW do front já usa, então a troca de mock→real não muda nada na UI.
- Segurança mínima de propósito: o próprio contrato documenta o token como
  "mock, no Spring real será um JWT" — trocar por JWT assinado (Nimbus/JJWT)
  é o próximo passo natural, não uma lacuna escondida.
- PIX com idempotência (header `Idempotency-Key`) e erro 409 semântico para
  saldo insuficiente (vs. 400 para input inválido) — mesma regra de negócio
  do mock, só que com debit atômico (`AtomicLong`/`synchronized`).
- Próximo passo de arquitetura: outbox pattern no PIX antes de publicar em
  Kafka/SQS, e JPA/Flyway no lugar do `InMemoryStore`.

## Pitch de 30s sobre WebFlux (para a entrevista)

"Adicionei um endpoint reativo (`Mono<BalanceResponse>`) coexistindo com a API
MVC existente, sem trocar Tomcat por Netty — a decisão consciente aqui foi não
reescrever a stack pra uma demo. Isso me dá espaço pra explicar a diferença
real: no MVC, cada request prende uma thread do pool enquanto espera I/O
(banco, chamada externa); no WebFlux, a thread é liberada e retomada quando o
dado chega, então a mesma máquina sustenta muito mais requisições concorrentes
com menos threads — o ganho aparece quando o gargalo é I/O concorrente (muitas
chamadas de rede, streaming), não em CRUD comum de baixo tráfego, onde MVC
blocking continua mais simples de escrever e debugar. Migração completa pra
non-blocking de ponta a ponta exigiria trocar o banco pra um driver reativo
(R2DBC) e subir o app inteiro em modo reativo — é um projeto, não uma tarde."

## Próximo micro-passo (se sobrar tempo)

- Alternativa: skeleton React equivalente ao Vue (login + saldo + extrato + PIX).
