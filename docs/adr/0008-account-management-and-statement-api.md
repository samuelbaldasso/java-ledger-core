# ADR 0008: Gestão de Contas, Extrato Bancário e Semântica de Conflito (`409 Conflict`)

- **Status:** Aceito
- **Data:** 2026-09-28
- **Decisores:** Samuel Baldasso

---

## 1. Contexto

Para que o sistema de ledger seja funcional de ponta a ponta, é necessário permitir a abertura de novas contas, a consulta de saldos consolidados e a emissão de extratos com o histórico de transações.
Essa camada introduz duas decisões de design REST:
1. **Tratamento de Colisão de Contas:** Como sinalizar adequadamente tentativas de cadastrar números de conta duplicados.
2. **Padrão de Criação de Recursos:** Como responder à criação bem-sucedida de um recurso REST com URI canônica.

---

## 2. Decisão

### 1. Semântica de Conflito (`409 Conflict`):
- Se o número de conta já existir no repositório, o caso de uso `OpenAccountUseCase` dispara `AccountNumberAlreadyExistsException`.
- O `GlobalExceptionHandler` mapeia essa exceção para **`409 Conflict`** via RFC 7807 (`ProblemDetail`), sinalizando formalmente colisão de estado com dados já existentes.

### 2. Resposta de Criação com Cabeçalho `Location` (`201 Created`):
- O endpoint `POST /api/v1/accounts` retorna **`201 Created`** acompanhado do cabeçalho de resposta:
  `Location: /api/v1/accounts/{id}`
- Isso segue rigorosamente o padrão REST, permitindo que clientes e navegadores descubram a URL canônica do novo recurso sem adivinhação.

### 3. Consultas de Saldo e Extrato:
- `GET /api/v1/accounts/{id}`: Retorna o snapshot atual do saldo e status da conta.
- `GET /api/v1/accounts/{id}/statement`: Retorna o histórico de transações registradas no livro-razão contábil para a conta especificada.

---

## 3. Consequências

### Positivas:
- **Alta Aderência aos Padrões REST:** Uso canônico de `201 Created`, `Location` header e `409 Conflict`.
- **Experiência Completa:** O sistema passa a oferecer o ciclo completo de onboarding, transações e auditoria contábil.
