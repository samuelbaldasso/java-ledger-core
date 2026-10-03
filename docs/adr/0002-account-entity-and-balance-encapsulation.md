# ADR 0002: Encapsulamento de Saldo e Invariantes na Entidade `Account`

- **Status:** Aceito
- **Data:** 2026-09-27
- **Decisores:** Samuel Baldasso

---

## 1. Contexto

A entidade `Account` representava a conta bancária no domínio, mas apresentava sérios problemas de encapsulamento:
1. **Setters Públicos no Saldo (`setBalance` / `setMoney`):** Permitiam que qualquer camada externa alterasse o saldo arbitrariamente sem passar pelas regras de negócio e validações de débito/crédito.
2. **Uso de Tipos Primitivos / BigDecimal Solto:** O saldo não utilizava o Value Object `Money`, permitindo operações sem validação de escala ou moeda.
3. **Falta de Proteção de Invariantes em Débito/Crédito:** Operações aceitavam valores negativos ou nulos, e misturavam validação de valor de entrada inválido com saldo insuficiente.
4. **Ciclo de Vida:** A abertura de conta não padronizava o estado inicial do saldo.

---

## 2. Decisão

Decidimos refatorar `Account` como uma **Entidade de Domínio Rica (Aggregate Root)**:

```java
public class Account
```

### Invariantes e Regras Definidas:

1. **Saldo como Value Object (`Money balance`):**
   - O saldo da conta é sempre um `Money`. A moeda da conta é a moeda do próprio saldo (`account.getCurrency() -> balance.currency()`).

2. **Remoção de Setters Públicos para Estado Crítico:**
   - O método `setBalance` foi eliminado. O saldo só pode ser mutado internamente através dos métodos de negócio `credit(Money amount)` e `debit(Money amount)`.

3. **Abertura Padronizada com Saldo Zero:**
   - O método de fábrica `openAccount(accountNumber, ownerName, currency)` cria contas ativas obrigatoriamente com saldo zero (`Money.zero(currency)`).
   - Para reidratação a partir do banco de dados (persistência), foi criado o método estático `reconstitute(...)`.

4. **Separação Estrita de Falhas em Transações:**
   - **Tentativa de transação não positiva:** Dispara `IllegalArgumentException` (ex: creditar ou debitar R$ 0 ou R$ -10).
   - **Tentativa de débito superior ao saldo:** Dispara `InsufficientFundsException(accountId, requested, currentBalance)`.
   - **Tentativa de transação com moeda divergente:** Dispara `CurrencyMismatchException` (delegada ao próprio `Money.add` / `Money.subtract`).
   - **Tentativa de transação em conta inativa/fechada:** Dispara `AccountNotActiveException`.

5. **Fechamento de Conta (`close()`):**
   - Uma conta só pode ser encerrada se seu saldo for estritamente zero (`balance.isZero()`).

---

## 3. Consequências

### Positivas:
- **Integridade Absoluta:** O saldo de uma conta nunca pode ser corrompido em memória por consumidores externos.
- **Auditoria e Transparência:** Todas as mudanças de saldo passam exclusivamente pelo fluxo contábil de débito/crédito.
- **Alinhamento com DDD:** `Account` atua como guardiã do seu agregado.

### Trade-offs:
- Camadas de persistência (JPA / Spring Data) não poderão utilizar setters reflexivos convencionais no modelo de domínio puro; precisarão de mappers específicos ou reidratação controlada via `reconstitute`.
