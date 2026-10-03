# ADR 0007: Design de API REST, Cabeçalho de Idempotência e Tratamento de Erros via RFC 7807 (`ProblemDetail`)

- **Status:** Aceito
- **Data:** 2026-09-28
- **Decisores:** Samuel Baldasso

---

## 1. Contexto

A borda externa da aplicação precisa expor endpoints HTTP REST para orquestrar transferências e operações bancárias.
Essa camada de interface apresenta três desafios críticos:
1. **Separação de Metadados de Rede vs Payload de Negócio:** Onde a chave de idempotência deve trafegar para não poluir o corpo da requisição de domínio.
2. **Semântica de Erros HTTP:** Evitar o antipadrão de retornar erros 200 com mensagem no corpo ou mascarar exceções de negócio como erros internos `500 Internal Server Error`.
3. **Padronização das Respostas de Erro:** Oferecer um formato estruturado, legível por máquinas e padronizado internacionalmente para diagnóstico de falhas em APIs públicas/parceiras.

---

## 2. Decisão

### 1. Cabeçalho HTTP `Idempotency-Key`:
- O cliente deve enviar o cabeçalho HTTP obrigatório:
  `Idempotency-Key: <UUID>`
- O controller captura esse header via `@RequestHeader("Idempotency-Key")` e o repassa ao `TransferMoneyCommand`. O payload JSON contém apenas os atributos financeiros (`sourceAccountId`, `targetAccountId`, `amount`, `currency`, `description`).

### 2. Mapeamento Semântico de Códigos HTTP:
- **`400 Bad Request`:** Erros de sintaxe ou dados de entrada inválidos (`MethodArgumentNotValidException`, `IllegalArgumentException`, `CurrencyMismatchException`, `MissingRequestHeaderException`).
- **`404 Not Found`:** Conta ou recurso inexistente (`AccountNotFoundException`).
- **`422 Unprocessable Entity`:** Requisição sintaticamente válida, mas rejeitada pelas regras de estado do domínio (`InsufficientFundsException`, `AccountNotActiveException`).
- **`500 Internal Server Error`:** Erros inesperados de infraestrutura ou falhas não capturadas.

### 3. Padrão RFC 7807 / RFC 9457 (`ProblemDetail`):
- Implementamos um `@RestControllerAdvice` utilizando a classe nativa do Spring Boot 3 `ProblemDetail`.
- As respostas de erro seguem o esquema padrão:
  - `type`: URI identificando o tipo do erro.
  - `title`: Título legível do erro.
  - `status`: Código HTTP correspondente.
  - `detail`: Mensagem contextual detalhada.
  - `instance`: URI da requisição que gerou o erro.

---

## 3. Consequências

### Positivas:
- **Conformidade com Padrões IETF:** Adoção formal dos padrões da web moderna (RFC 7807 e rascunho IETF Idempotency-Key).
- **Sem Vazamento de Stack Trace:** Nenhuma informação interna do servidor vaza em produção.
- **Transparência para Clientes e Frontend:** Mensagens claras distinguem erros de digitação (400), contas erradas (404) e falta de saldo (422).
