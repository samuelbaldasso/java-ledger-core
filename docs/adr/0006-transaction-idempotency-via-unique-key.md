# ADR 0006: Garantia de Idempotência em Transações Financeiras via Chave Única

- **Status:** Aceito
- **Data:** 2026-09-28
- **Decisores:** Samuel Baldasso

---

## 1. Contexto

Em redes móveis e ambientes distribuídos, falhas de conectividade (timeout, oscilação de sinal, perda de pacotes) geram retransmissões automáticas ou cliques repetidos de usuários.
Sem proteção de idempotência:
- Um cliente tentando transferir R$ 100,00 pode sofrer múltiplos débitos caso sua conexão caia após o backend processar a primeira chamada e o cliente tente reenviar a requisição.
- Isso gera inconsistência financeira, contestação de débitos e atrito com clientes.

---

## 2. Decisão

Decidimos implementar o padrão **Idempotency Key (Chave de Idempotência)** na criação de transações contábeis:

### 1. Modelagem Relacional (Flyway V2):
- Adicionamos a coluna `idempotency_key VARCHAR(128) NOT NULL` na tabela `transactions`.
- Criamos um índice único (`CREATE UNIQUE INDEX idx_transactions_idempotency_key ON transactions(idempotency_key)`), garantindo que o PostgreSQL impeça qualquer duplicação física.

### 2. Contrato de Domínio:
- A entidade `Transaction` exige uma `idempotencyKey` não nula e não em branco no momento de sua criação.
- O repositório de domínio expõe a consulta:
  `Optional<Transaction> findByIdempotencyKey(String idempotencyKey)`

### 3. Orquestração no Caso de Uso (`TransferMoneyService`):
- O comando de transferência passa a exigir a `idempotencyKey`.
- **Verificação Prévia:** Antes de adquirir locks pessimistas nas contas, o serviço consulta se a `idempotencyKey` já foi processada.
  - Se **SIM**: O serviço retorna imediatamente o resultado da transação previamente executada (Status 200 OK sem efeito colateral duplicado).
  - Se **NÃO**: Prossegue com o lock ordering e execução da transferência.
- **Tratamento de Colisão Concorrente:** Em caso de duas requisições com a mesma chave chegando no mesmo milissegundo, a segunda requisição é interceptada pela constraint única do banco, recuperando e retornando a transação vencedora com segurança.

---

## 3. Consequências

### Positivas:
- **Segurança de Rede e Retentativas:** Clientes e gateways podem retransmitir requisições com segurança total sem risco de cobrança duplicada.
- **Conformidade com Padrões Financeiros:** Padrão alinhado às diretrizes do Pix (Banco Central do Brasil) e gateways modernos (Stripe).

### Trade-offs:
- Todo consumidor da API precisa gerar e fornecer um identificador único (geralmente UUID v4) por intenção de pagamento.
