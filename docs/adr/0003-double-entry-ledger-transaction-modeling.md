# ADR 0003: Modelagem de Livro-Razão Contábil por Partidas Dobradas (`Transaction` e `Entry`)

- **Status:** Aceito
- **Data:** 2026-09-28
- **Decisores:** Samuel Baldasso

---

## 1. Contexto

Em sistemas financeiros, permitir que transferências ocorram apenas atualizando saldos de duas contas isoladas (`source.debit()` e `target.credit()`) traz problemas críticos:
1. **Ausência de integridade matemática:** Não há como provar matematicamente que o valor debitado de uma conta foi integralmente creditado em outra(s).
2. **Falta de auditabilidade contábil:** Sem um registro formal de partidas dobradas, a reconciliação e a auditoria externa tornam-se frágeis e suscetíveis a inconsistências silenciosas.
3. **Rigidez para lançamentos compostos:** Cenários comuns como transferências com taxa (fees), splits de pagamento e estornos parciais tornam-se complexos e propensos a bugs.

---

## 2. Decisão

Decidimos modelar as operações financeiras como um **Livro-Razão (Ledger) baseado no princípio secular das Partidas Dobradas (Double-Entry Bookkeeping)**:

### Componentes de Domínio:

1. **`EntryType`:**
   - Enum representando a natureza contábil da perna do lançamento: `DEBIT` e `CREDIT`.

2. **`Entry`:**
   - Representa uma perna individual de um lançamento contábil.
   - Atributos: `UUID id`, `UUID accountId`, `EntryType type`, `Money amount`.
   - Invariante: O `amount` deve ser estritamente positivo (o sentido contábil é definido exclusivamente pelo `type`).

3. **`Transaction` (Aggregate Root):**
   - Agregado que encapsula o conjunto atômico de `Entry`s de um evento contábil.
   - Atributos: `UUID id`, `String description`, `List<Entry> entries`, `Instant createdAt`.
   - **Invariante Central (Equilíbrio Contábil):**
     $$\sum \text{amount}(\text{DEBIT}) == \sum \text{amount}(\text{CREDIT})$$
   - Se a soma dos débitos diferir da soma dos créditos, a transação é rejeitada com a exceção `UnbalancedTransactionException`.
   - Todas as pernas (`Entry`) da mesma transação devem pertencer à mesma moeda.
   - Uma transação deve possuir no mínimo duas pernas (`entries.size() >= 2`).

4. **Relação com o Saldo (`Account.balance`):**
   - Adotamos a arquitetura **Híbrida (Ledger Append-only + Snapshot na Conta)**:
     - As `Entry`s do Ledger são o registro histórico oficial, imutável e append-only.
     - O campo `Account.balance` é atualizado como um *snapshot* de alta performance para permitir leituras O(1) de saldo disponível sem varredura completa do histórico.

---

## 3. Consequências

### Positivas:
- **Conservação de Dinheiro:** Impossível criar ou destruir dinheiro sem uma contrapartida explícita no sistema.
- **Suporte Nativo a Operações Complexas:** Transferências com taxas de banco, pagamentos divididos (splits) e liquidações operam sob o mesmo modelo unificado.
- **Auditabilidade Padrão Bancário:** Compatível com padrões internacionais de contabilidade e exigências regulatórias.

### Trade-offs:
- Toda operação de movimentação financeira exige a montagem de um conjunto equilibrado de entradas contábeis, aumentando ligeiramente o volume de registros no banco de dados.
