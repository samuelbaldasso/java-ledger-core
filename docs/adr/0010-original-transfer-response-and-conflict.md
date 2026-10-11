# ADR 0010: Devolver a resposta original e rejeitar mudanças na requisição

- **Status:** Aceito
- **Data:** 2026-10-11
- **Decisor:** Samuel Baldasso
- **Relacionados:** ADR 0006, ADR 0011 e ADR 0012

## Problema

O cliente envia R$ 100 de A para B. O banco confirma a transferência, mas a resposta se perde na rede.
Ao tentar de novo, o cliente precisa saber o que aconteceu naquela operação. Consultar o saldo atual
não reconstrói a resposta: outras transferências podem ter ocorrido depois.

A implementação anterior misturava o ID antigo com o valor e as contas da nova requisição.

## Alternativas e escolha

1. Devolver o resultado original, inclusive os saldos daquele momento.
2. Devolver apenas os dados da operação registrada e retirar os saldos históricos da resposta.

Samuel escolheu a primeira opção. A resposta funciona como um comprovante: pedir uma segunda via
não altera o conteúdo da primeira.

## Contrato

- Mesma chave e mesma requisição: HTTP 200 com os mesmos campos da resposta original.
- Mesma chave com outra origem, destino, valor, moeda ou descrição: exceção
  `IdempotencyConflictException`, convertida em HTTP 409.
- A descrição é comparada exatamente; não removemos espaços nem mudamos letras.
- Valores como 100.0 e 100.00 são equivalentes. A moeda recebida pela API é normalizada para maiúsculas.
- A API rejeita casas decimais que exigiriam arredondamento, com HTTP 400.
  Assim, 100.001 BRL não vira silenciosamente a mesma intenção que 100.00 BRL.
- A criação da transferência usa precisão de microssegundos, compatível com a persistência,
  para que o horário não mude entre a primeira resposta e sua segunda via.
- Apenas resultados de sucesso são armazenados. Uma tentativa que falhou pode ser repetida.

## Trade-off

Ganhamos uma resposta previsível após perda de conexão. Em troca, guardamos os saldos resultantes,
mesmo que já sejam diferentes dos saldos atuais. Para consultar o saldo de agora, usa-se a API de contas.

## Validação

`TransferReceiptTest` verifica equivalência monetária e alterações em cada campo.
`TransferIdempotencyIntegrationTests` compara as respostas HTTP após outra movimentação e mudança
de status da conta, além de verificar HTTP 409 para valor diferente e HTTP 400 para precisão inválida.
