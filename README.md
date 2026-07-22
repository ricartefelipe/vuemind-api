# VueMind API

Backend Spring Boot da carteira digital de estudo **VueMind Wallet**. Implementa
o contrato em `docs/contracts/vuemind-wallet-openapi.yaml` (repositório
`vuemind`) com dados em memória — skeleton propositalmente mínimo, sem
banco/mensageria/cloud ainda.

## Stack

- Java 21 + Spring Boot 3.3 (Web, Security)
- Sem banco: `InMemoryStore` (listas + `AtomicLong` + `ConcurrentHashMap`)
- Sem JWT real: token opaco fixo (`mock-jwt-demo`), igual ao mock MSW do front
- Testes: MockMvc (`spring-boot-starter-test`)

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

Autenticação: enviar `Authorization: Bearer mock-jwt-demo` (token fixo
devolvido pelo login).

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

## Próximo micro-passo (se sobrar tempo)

- Um endpoint em WebFlux (`GET /api/v1/wallet/balance` como `Mono<BalanceResponse>`
  num módulo/perfil separado) para falar de non-blocking na entrevista.
- Alternativa: skeleton React equivalente ao Vue (login + saldo + extrato + PIX).
