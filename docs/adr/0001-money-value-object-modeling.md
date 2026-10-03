# ADR 0001: Modelagem de Valores Monetários como Value Object (`Money`)

- **Status:** Aceito
- **Data:** 2026-09-27
- **Decisores:** Samuel Baldasso

---

## 1. Contexto

Sistemas bancários e de contabilidade financeira (ledgers) exigem tratamento estrito de precisão, arredondamento e integridade monetária. Tipos primitivos (`double`, `float`) introduzem erros de ponto flutuante inaceitáveis para transações financeiras.

Além disso, utilizar `BigDecimal` puro solto pelo sistema apresenta dois riscos graves:
1. **Comparações incorretas de igualdade:** No Java, `new BigDecimal("10.0").equals(new BigDecimal("10.00"))` é `false`, pois o método `equals` considera o valor e a escala.
2. **Operações entre moedas distintas:** Nada impede acidentalmente somar um valor em BRL com um valor em USD se ambos forem apenas instâncias de `BigDecimal`.
3. **Falta de encapsulamento de invariantes:** Regras como arredondamento bancário (`HALF_EVEN`) e número oficial de casas decimais ficam dispersas por services e controllers.

---

## 2. Decisão

Decidimos modelar valores monetários utilizando um **Value Object** explícito no domínio: a classe `Money`, implementada como um `record` imutável do Java 21:

```java
public record Money(BigDecimal amount, Currency currency) implements Comparable<Money>
```

### Invariantes e Regras Definidas:

1. **Imutabilidade e Semântica de Value Object:**
   - Implementado como `record` do Java 21. Não possui identidade (`id`). A igualdade é estrutural e baseada em seus campos normalizados.
   
2. **Rejeição Estrita de Nulos:**
   - Tanto `amount` quanto `currency` são validados no construtor compacto (`Objects.requireNonNull`), lançando exceção imediatamente caso sejam nulos.

3. **Normalização Dinâmica de Escala por Moeda (ISO 4217):**
   - Para evitar acoplamento com hardcode de 2 casas decimais e permitir suporte extensível a qualquer moeda mundial (ex: `BRL` e `USD` com 2 casas, `JPY` com 0 casas, `BHD` com 3 casas), a escala é obtida dinamicamente via `currency.getDefaultFractionDigits()`.
   - O arredondamento é padronizado em `RoundingMode.HALF_EVEN` (arredondamento bancário), que minimiza viés estatístico em operações cumulativas.

4. **Proteção Contra Operações Heterogêneas (Cross-Currency):**
   - Operações aritméticas (`add`, `subtract`) e de comparação (`isGreaterThan`, `compareTo`) exigem estritamente a mesma moeda.
   - Qualquer tentativa de operar moedas distintas lança uma exceção de domínio explícita: `CurrencyMismatchException`.

5. **Representação de Valores:**
   - O `Money` em si pode representar valores positivos, zero ou negativos (permitindo expressar saldos devedores ou saldos consolidados).
   - Métodos utilitários de estado (`isPositive()`, `isZero()`, `isNegative()`, `isGreaterThanOrEqual()`) são expostos para evitar vazamento de comparações diretas de `BigDecimal`.

---

## 3. Consequências

### Positivas:
- **Zero Vazamento de Escala:** Dois objetos `Money` com mesmo valor numérico e mesma moeda sempre serão estritamente iguais via `.equals()`, independentemente de como o `BigDecimal` de entrada foi formatado.
- **Segurança de Domínio:** Impossível misturar moedas sem conversão cambial explícita.
- **Alta Legibilidade:** Métodos expressivos (`money.isGreaterThan(balance)`) em vez de comparações verbosas de `compareTo(BigDecimal.ZERO)`.
- **Extensibilidade:** Suporte nativo e automático a qualquer moeda sem necessidade de refatoração do código existente.

### Trade-offs:
- Todo cálculo monetário gera uma nova instância de `Money` (devido à imutabilidade). Na JVM moderna, esses objetos têm ciclo de vida curto (geração jovem / escape analysis) e custo desprezível perto dos benefícios de integridade.
- Operações de divisão/rateio exigem regras específicas de distribuição de centavos residuais caso venham a ser implementadas no futuro.
