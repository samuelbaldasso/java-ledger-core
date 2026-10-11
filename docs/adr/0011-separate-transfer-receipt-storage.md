# ADR 0011: Guardar o comprovante de idempotência separado do ledger

- **Status:** Aceito
- **Data:** 2026-10-11
- **Decisor:** Samuel Baldasso
- **Relacionados:** ADR 0010 e ADR 0012

## Problema

Precisamos guardar o pedido original e os dados necessários para devolver a mesma resposta.
O ledger já registra a movimentação em `transactions` e `entries`, mas não guardava os saldos
resultantes presentes na resposta HTTP.

## Alternativas e escolha

1. Acrescentar os saldos resultantes ao registro contábil.
2. Criar um registro separado para a requisição e seu resultado.

Samuel escolheu a segunda opção. O ledger é o livro das movimentações; o registro de idempotência
guarda o comprovante que o cliente pode pedir novamente.

Essa separação organiza responsabilidades. Ela, sozinha, não garante maior capacidade de escala.

## Implementação

A migration V3 cria `transfer_receipts`, com chave única, referência à transação, origem, destino,
valor, moeda, descrição, saldos resultantes e horário. O valor e as contas servem tanto à comparação
da requisição quanto à reconstrução do resultado.

A aplicação usa `TransferReceiptRepository`; o adapter JPA só oferece consulta e inserção.
A inserção usa `persist`, sem atualizar um registro vencedor. Campos persistidos não são atualizáveis
pelo mapeamento. Isso não equivale a proibir alterações por SQL direto.

Saldos, lançamentos e comprovante são gravados na mesma transação de banco:
ou tudo é confirmado, ou tudo é desfeito.

O índice único já existente em `transactions.idempotency_key` é mantido para proteger também
chaves históricas. Não apagamos dados nem reescrevemos as migrations V1 e V2.

## Compatibilidade com registros antigos

Não sabemos os saldos exatos que uma resposta antiga devolveu. Não há backfill confiável desses dados.
Uma chave registrada no ledger, mas sem comprovante, retorna HTTP 409 com o tipo
`idempotency-result-unavailable`. A transferência não é executada novamente.

Essa é uma proteção de compatibilidade; não se deve contorná-la repetindo o mesmo pagamento com outra chave.
A investigação do registro antigo é necessária.

## Trade-off

Ganhamos separação entre contabilidade e repetição de respostas. Pagamos com armazenamento adicional,
mais uma gravação por transferência e a obrigação de manter os registros na mesma transação.

Os registros não têm expiração nesta implementação. Definir retenção e limpeza é uma decisão futura;
apagar uma chave pode comprometer a proteção contra repetição.

## Validação

O teste de integração provoca uma falha depois de gravar e enviar todas as alterações ao banco
(`flush`). Verifica então que o rollback remove o comprovante e os lançamentos e restaura os saldos.
Outro teste simula uma chave antiga e verifica que ela não movimenta dinheiro de novo.
