# ADR 0006: Garantia de Idempotência em Transações Financeiras via Chave Única

- **Status:** Parcialmente substituído pelos ADRs 0010, 0011 e 0012
- **Data:** 2026-09-28
- **Decisores:** Samuel Baldasso

> Atualização em 2026-10-11: a chave única permanece. O contrato de resposta, o armazenamento
> separado e a recuperação após rollback estão detalhados nos [ADRs 0010](0010-original-transfer-response-and-conflict.md),
> [0011](0011-separate-transfer-receipt-storage.md) e [0012](0012-idempotency-collision-recovery-after-rollback.md).
> O texto original descrevia uma recuperação de colisões que ainda não estava implementada.

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
- **Tratamento de Colisão Concorrente:** a restrição única impede duas transações confirmadas com a mesma chave. A recuperação da resposta vencedora exige encerrar o rollback antes de fazer uma nova consulta; esse fluxo foi implementado conforme o ADR 0012.

---

## 3. Consequências

### Positivas:
- **Retentativas:** a chave única protege contra duplicação de transações confirmadas. Resposta original, divergência de dados e registros históricos seguem os contratos dos ADRs 0010 a 0012.
- A presença de idempotência não comprova conformidade regulatória nem elimina todas as falhas de rede ou de operação.

### Trade-offs:
- Todo consumidor da API precisa gerar e fornecer um identificador único (geralmente UUID v4) por intenção de pagamento.
