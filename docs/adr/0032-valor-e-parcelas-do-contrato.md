# 0032 — Valor e parcelas do contrato calculados pelas linhas travadas

- **Status:** aceito
- **Data:** 2026-11-12

## Contexto

O contrato não tinha valor: a tela mostrava o valor da sessão de cada item, mas não o total do
contrato nem como ele seria pago. A clínica parcela os contratos (ex: 3x, 6x), em qualquer
forma de pagamento, sem juros. Os itens já travam a linha de preço
([ADR 0008](0008-contrato-com-itens.md)), e o prazo vem da vigência calculada
([ADR 0009](0009-vigencia-calculada.md)). A tela precisa ver o efeito de cada escolha (plano,
itens, parcelas) antes de salvar, sem repetir no front a regra de preço.

## Decisão

- **Valor de cada item:** sessões × valor da sessão da linha travada. Item por duração: as
  sessões do plano. Item do Pilates em Grupo: semanas do plano do Grupo × vezes por semana.
  **Total:** a soma dos itens, sem desconto. No `ContractItemValue` (Value Object).
- **Parcelas:** de 1 até os meses do contrato, no máximo 12. **Meses pelas semanas** da
  vigência (52 semanas = 12 meses), arredondados para o mais próximo. **Centavos que sobram na
  primeira parcela.** No `Installments` (Value Object).
- **Só o número de parcelas é gravado** (`installments`, `CHECK` de 1 a 12). O total e o valor
  das parcelas são calculados a cada leitura, a partir das linhas travadas.
- **Na criação**, parcelas acima do máximo dão 400. **Na prévia**, valem o máximo, e a resposta
  diz as aplicadas (a tela não perde o resultado quando o plano muda para um mais curto).
- **Prévia por `POST /api/contracts/preview`**, com o plano, o início, os itens e as parcelas,
  no lugar do `GET /api/contracts/term-preview` (que recebia só o plano e as frequências).
  Devolve o fim, as opções do plano do Grupo, o valor de cada item, o total e as parcelas. O
  [ADR 0010](0010-plano-do-item-do-grupo.md) continua valendo: só o endpoint que devolve as
  opções mudou.
- **Aditivo:** copia as parcelas da versão anterior.

## Alternativas descartadas

- **Gravar o total do contrato:** poderia ficar diferente da soma dos itens. As linhas travadas
  não mudam, então o total calculado é sempre o mesmo.
- **Meses pelo calendário** (do início ao fim): o mesmo plano daria máximos diferentes conforme
  o início (26 semanas são 6 meses e 1 dia, que arredondados para cima seriam 7).
- **Calcular o valor e as parcelas no front:** regra de preço é do backend (princípio 1 do
  front, no `architecture.md`).
- **Prévia por query string no `GET`:** os itens (técnica, linha e frequência de cada um) não
  cabem bem em parâmetros de URL; o corpo do `POST` usa o mesmo formato do cadastro.

## Consequências

- A resposta do contrato traz `totalValue`, `installments`, `firstInstallmentValue` e
  `installmentValue`; cada item traz `sessionCount` e `totalValue`.
- A V1 foi editada ([ADR 0028](0028-migrations-editaveis-ate-versao-estavel.md)): quem puxar
  zera o banco local.
- Renegociar o parcelamento na prorrogação fica para a spec do aditivo.
- Desconto, juros e entrada não existem; se a clínica passar a usá-los, esta decisão é revista.
