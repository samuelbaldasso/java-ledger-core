# ADR 0012: Recuperar colisões de idempotência depois do rollback

- **Status:** Aceito
- **Data:** 2026-10-11
- **Decisor:** Samuel Baldasso
- **Relacionados:** ADR 0005, ADR 0010 e ADR 0011

## Problema

Duas requisições podem consultar a mesma chave antes de qualquer uma terminar.
Ambas enxergam que a chave ainda não existe. Consultar antes de executar não basta para impedir a disputa.

## Alternativas e escolha

1. Reservar a chave antes de movimentar as contas.
2. Tentar executar e resolver a colisão quando a restrição única impedir a gravação duplicada.

Samuel escolheu a segunda opção, aceitando o trabalho que poderá ser desfeito.

A garantia de não deixar alterações parciais vem da transação e do rollback. A primeira opção também
pode oferecer essa garantia. A escolha não elimina espera: locks e índices únicos podem fazer
uma requisição esperar pela outra.

## Fluxo

1. Consultar o comprovante. Se existir, comparar o pedido e devolver a resposta ou HTTP 409.
2. Proteger chaves antigas que ainda não possuem comprovante.
3. Bloquear as contas na ordem determinística já adotada pelo projeto.
4. Consultar novamente o comprovante depois de obter os locks.
5. Debitar, creditar, registrar os lançamentos e inserir o comprovante na mesma transação.
6. Se ocorrer colisão na persistência, deixar a tentativa sair com erro e sofrer rollback completo.
7. Só depois consultar o comprovante vencedor em uma nova transação.
8. Devolver a resposta original para pedidos iguais ou HTTP 409 para pedidos diferentes.

A segunda consulta do passo 4 é importante: enquanto esperava pelas contas, a requisição pode ter
sido atendida pela vencedora. Se esta gastou todo o saldo, precisamos devolver o comprovante,
em vez de tentar debitar de novo e responder incorretamente que faltou dinheiro.

## Limites de transação

`TransferExecutionService` executa a tentativa com `REQUIRES_NEW` e `READ_COMMITTED`.
`TransferMoneyService` chama esse serviço por outro bean Spring e captura o erro fora da tentativa.
A busca após colisão também abre uma nova transação.

Em linguagem simples: primeiro encerramos e desfazemos a tentativa que deu errado; depois abrimos
outra conversa com o banco para ler o resultado de quem conseguiu.

O ponto de entrada representa uma transferência independente. Se outro serviço já estiver dentro de
uma transação, a transferência não será desfeita pelo rollback desse serviço externo. Ela não deve
ser usada como parte de uma operação maior que exija um único commit.

Uma falha de integridade só vira replay se existir um comprovante confirmado para aquela chave.
Sem comprovante, o erro original continua sendo propagado. Não repetimos automaticamente a movimentação.

## Trade-off e limites

Ganhamos recuperação de colisões sem uma reserva prévia. Aceitamos trabalho perdido, espera por locks,
consultas adicionais e uma separação mais explícita dos limites de transação.
Isso não prova melhora de performance: latência e capacidade precisam ser medidas.

A consulta de recuperação só acontece após a tentativa anterior encerrar. Chamadas dentro de transações
externas ainda exigem cuidado com conexões e locks. Deadlocks gerais, timeouts e falhas de rede não recebem
uma política automática de retentativa nesta decisão.

## Validação

- Duas requisições iguais concorrem por saldo suficiente para apenas uma transferência: ambas recebem
  o mesmo resultado e existem apenas uma transação, dois lançamentos e um comprovante.
- Duas requisições com a mesma chave e contas diferentes chegam juntas à persistência: uma vence;
  a outra recebe conflito e suas contas mantêm os saldos originais.
- Uma falha provocada após todas as gravações desfaz também o comprovante.
- Falha de integridade sem comprovante vencedor não é escondida.

As disputas dos testes são coordenadas por barreiras, sem depender de pausas arbitrárias.

Referência: [propagação de transações no Spring](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/tx-propagation.html).
