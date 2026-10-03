# Documentação de Arquitetura e Decisões Técnicas (ADRs)

Este diretório contém os registros de decisões arquiteturais (**Architecture Decision Records — ADRs**) do projeto `ledger-core`.

## Índice de Decisões

| ADR | Título | Status | Data |
|---|---|---|---|
| [ADR-0001](adr/0001-money-value-object-modeling.md) | Modelagem de Valores Monetários como Value Object (`Money`) | Aceito | 2026-09-27 |
| [ADR-0002](adr/0002-account-entity-and-balance-encapsulation.md) | Encapsulamento de Saldo e Invariantes na Entidade `Account` | Aceito | 2026-09-27 |
| [ADR-0003](adr/0003-double-entry-ledger-transaction-modeling.md) | Modelagem de Livro-Razão Contábil por Partidas Dobradas (`Transaction` e `Entry`) | Aceito | 2026-09-28 |
| [ADR-0004](adr/0004-persistence-architecture-postgresql-and-pessimistic-locking.md) | Arquitetura de Persistência com PostgreSQL, Flyway e Pessimistic Locking | Aceito | 2026-09-28 |
| [ADR-0005](adr/0005-deadlock-prevention-via-deterministic-lock-ordering.md) | Prevenção de Deadlock por Ordenação Determinística de Locks em Transferências | Aceito | 2026-09-28 |
| [ADR-0006](adr/0006-transaction-idempotency-via-unique-key.md) | Garantia de Idempotência em Transações Financeiras via Chave Única | Aceito | 2026-09-28 |
| [ADR-0007](adr/0007-rest-api-design-and-rfc-7807-error-handling.md) | Design de API REST, Cabeçalho de Idempotência e Tratamento de Erros via RFC 7807 (`ProblemDetail`) | Aceito | 2026-09-28 |
| [ADR-0008](adr/0008-account-management-and-statement-api.md) | Gestão de Contas, Extrato Bancário e Semântica de Conflito (`409 Conflict`) | Aceito | 2026-09-28 |
| [ADR-0009](adr/0009-openapi-documentation-and-containerization.md) | Documentação Interativa com OpenAPI/Swagger e Containerização com Docker | Aceito | 2026-09-28 |
