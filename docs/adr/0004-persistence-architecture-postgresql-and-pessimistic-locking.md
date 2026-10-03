# ADR 0004: Arquitetura de Persistência com PostgreSQL, Flyway e Pessimistic Locking

- **Status:** Aceito
- **Data:** 2026-09-28
- **Decisores:** Samuel Baldasso

---

## 1. Contexto

Após definirmos o modelo de domínio para `Account`, `Money`, `Transaction` e `Entry`, precisamos projetar a estratégia de persistência relacional.
Sistemas bancários exigem:
1. **Precisão numérica absoluta:** Impossibilidade de erros de truncamento ou ponto flutuante no banco.
2. **Isolamento de Domínio (Clean Architecture):** As regras de negócio não devem ser contaminadas por particularidades e anotações do framework de ORM (Hibernate/JPA).
3. **Controle de Concorrência Crítico:** Se duas operações de débito atingirem a mesma conta simultaneamente em threads diferentes, ambas poderiam ler o mesmo saldo e levar a conta a um saldo negativo indevido (Race Condition / Lost Update).

---

## 2. Decisão

### 1. Modelagem Relacional (DDL via Flyway):
- Criamos a migração `V1__create_accounts_and_ledger_tables.sql` contendo:
  - `accounts`: Chave primária `UUID`, `account_number VARCHAR(32) UNIQUE`, `balance NUMERIC(19, 4)` com check constraint `balance >= 0`, `currency VARCHAR(3)`, `status VARCHAR(16)`, `created_at TIMESTAMP WITH TIME ZONE`.
  - `transactions`: Cabeçalho imutável com `UUID`, `description VARCHAR(255)`, `created_at TIMESTAMP WITH TIME ZONE`.
  - `entries`: Pernas individuais contábeis com `UUID`, `transaction_id REFERENCES transactions(id) ON DELETE RESTRICT`, `account_id REFERENCES accounts(id) ON DELETE RESTRICT`, `entry_type VARCHAR(8) CHECK (entry_type IN ('DEBIT', 'CREDIT'))`, `amount NUMERIC(19, 4) CHECK (amount > 0)`, `currency VARCHAR(3)`.
  - Chaves estrangeiras com `ON DELETE RESTRICT` para garantir natureza estritamente **append-only**.
  - Índices em `entries(transaction_id)` e `entries(account_id)`.

### 2. Clean Architecture no Mapeamento (Separação de Entidades):
- O modelo de domínio puro permanece no pacote `domain.model`.
- Criamos entidades JPA dedicadas no pacote de infraestrutura:
  - `AccountJpaEntity`
  - `TransactionJpaEntity`
  - `EntryJpaEntity`
- Mappers explícitos (`AccountEntityMapper`, `TransactionEntityMapper`) convertem entre o modelo relacional JPA e os agregados puros de domínio (utilizando `Account.reconstitute(...)`).

### 3. Estratégia de Concorrência: Pessimistic Locking (`SELECT ... FOR UPDATE`):
- Para operações que alteram saldo (`debit`, `credit`), a busca da conta no banco é feita via `findByIdForUpdate` anotada com `@Lock(LockModeType.PESSIMISTIC_WRITE)`.
- Isso garante que o PostgreSQL trave a linha da conta no nível de banco até o término da transação `@Transactional`, serializando operações concorrentes e eliminando condições de corrida.

### 4. Testes de Integração com Testcontainers:
- Usamos `PostgreSQLContainer` gerenciado pelo Spring Boot 3.3 via `@ServiceConnection`, garantindo que os testes de integração e migrações do Flyway rodem contra uma instância real de PostgreSQL.

---

## 3. Consequências

### Positivas:
- **Proteção contra Race Conditions:** O bloqueio pessimista elimina risco de saldo negativo por concorrência descontrolada.
- **Domínio Livre de Acoplamento:** Nenhuma dependência do Hibernate no domínio (`com.sbaldasso.ledgercore.domain`).
- **Garantias em Nível de SGBD:** Constraints SQL e tipos `NUMERIC(19, 4)` protegem os dados mesmo contra scripts externos ou intervenções manuais.

### Trade-offs:
- **Pessimistic Locking reduz concorrência máxima de uma única conta:** Duas operações na mesma conta são enfileiradas. Em sistemas bancários, isso é o comportamento desejado para garantir consistência estrita de saldo.
- **Necessidade de código de mapeamento:** Manter `*JpaEntity` e `*Mapper` adiciona classes intermediárias de infraestrutura.
