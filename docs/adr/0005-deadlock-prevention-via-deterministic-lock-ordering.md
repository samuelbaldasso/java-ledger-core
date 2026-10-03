# ADR 0005: Prevenção de Deadlock por Ordenação Determinística de Locks em Transferências

- **Status:** Aceito
- **Data:** 2026-09-28
- **Decisores:** Samuel Baldasso

---

## 1. Contexto

Em transferências bancárias concorrentes, duas transações simultâneas envolvendo as mesmas contas em sentidos opostos (ex: Conta A -> Conta B e Conta B -> Conta A) podem adquirir bloqueios pessimistas (`SELECT ... FOR UPDATE`) em ordem cruzada:
1. Transação 1 bloqueia Conta A e tenta bloquear Conta B.
2. Transação 2 bloqueia Conta B e tenta bloquear Conta A.

Essa situação resulta em **Deadlock mútuo** no banco de dados. O PostgreSQL é forçado a intervir via detector de deadlock, cancelando e abortando uma das transações com erro em tempo de execução.

---

## 2. Decisão

Decidimos aplicar a estratégia de **Ordenação Determinística de Locks (Lock Ordering)** na camada de aplicação (`TransferMoneyUseCase`):

### Regras de Execução:

1. **Ordenação Lexicográfica dos Identificadores:**
   - Antes de solicitar qualquer lock ao banco de dados, o caso de uso compara os UUIDs das contas de origem e destino (`sourceAccountId.compareTo(targetAccountId)`).
   - O lock é adquirido **sempre na mesma ordem global** (primeiro a conta com menor UUID, depois a conta com maior UUID), independentemente de quem é origem ou destino na operação financeira.

```text
Lock 1: min(sourceId, targetId) -> findByIdForUpdate(...)
Lock 2: max(sourceId, targetId) -> findByIdForUpdate(...)
```

2. **Garantia Matemática:**
   - Como todas as transações do sistema concorrem pelos mesmos recursos seguindo a mesma hierarquia estrita de aquisição, **ciclos de espera são matematicamente impossíveis**, eliminando a ocorrência de deadlocks entre transferências simultâneas das mesmas contas.

3. **Arquitetura de Casos de Uso:**
   - A camada de aplicação expõe uma interface de contrato (`TransferMoneyUseCase`) desacoplada de sua implementação concreta (`TransferMoneyService`), recebendo um comando imutável (`TransferMoneyCommand`) e retornando um resultado de transferência (`TransferResult`).

---

## 3. Consequências

### Positivas:
- **Zero Deadlocks por Concorrência Cruzada:** Elimina abortos inesperados de transações e erros 500 para clientes que transferem simultaneamente.
- **Previsibilidade Operacional:** Transações concorrentes entram em fila limpa de serialização no PostgreSQL.

### Trade-offs:
- O caso de uso precisa de uma lógica simples de remapeamento pós-busca para identificar qual entidade bloqueada corresponde à origem (`source`) e qual ao destino (`target`).
