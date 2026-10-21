# 0003 — Preço historizado por `validFrom`/`validTo` exclusivo

- **Status:** aceito
- **Data:** 2026-10-20 (registro retroativo)

## Contexto

Os preços da clínica são reajustados. Contratos já assinados precisam continuar
apontando para o valor combinado, e o histórico de preços é requisito de
rastreabilidade do escopo do projeto.

## Decisão

- **Reajuste nunca é `UPDATE`.** Fecha a linha vigente (`validTo` = hoje) e insere
  uma nova com `validFrom` = hoje. Um único fluxo cobre preço novo e reajuste:
  "a partir de hoje, essa combinação custa X".
- **`validTo` é exclusivo:** a linha vale no intervalo `[validFrom, validTo)`.
  Consulta "preço vigente na data X": `validFrom <= X AND (validTo IS NULL OR
  validTo > X)`.
- **`validFrom` é sempre hoje**, nunca vem do request.
- Só uma linha vigente (`validTo IS NULL`) por grupo + plano + eixo (duração ou
  frequência).

## Alternativas descartadas

- **`UPDATE` direto:** perde o histórico, e o contrato passaria a apontar para um
  valor que não foi o combinado.
- **`validTo` inclusivo:** obrigaria a conta do "ontem" ao fechar a linha, e um
  erro nessa conta deixaria dois preços valendo no mesmo dia. Exclusivo, a antiga
  fecha e a nova abre na **mesma data**.
- **Tabela de preço anual** (entidade `PriceTable`, ex: "Tabela 2026", com as
  linhas apontando para ela no lugar de `validFrom`/`validTo`): foi pensada para
  achar o preço "do mesmo ano" do contrato quando o paciente usa uma técnica de
  outro grupo. Ficou desnecessária com a regra decidida: **o que foi contratado
  mantém o preço travado; o resto paga o preço atual.** Sem precisar relacionar
  preços de grupos diferentes por ano, as linhas individuais bastam.
- **`validFrom` informado (reajuste agendado):** como vigente = `validTo` nulo, uma
  linha com início futuro já contaria como vigente antes da hora. Fica como
  melhoria futura.

## Consequências

- ⚠️ O `endDate` do contrato é **inclusivo** (o último dia conta), ao contrário do
  `validTo`. Atenção ao comparar os dois.
- Dois reajustes no mesmo dia geram uma linha de intervalo vazio
  (`[15/10, 15/10)`). Não é inválido e não há bloqueio: corrigir um valor
  digitado errado é só cadastrar de novo.
- A unicidade da linha vigente hoje é checada no código; a garantia no banco
  (coluna gerada + `UNIQUE`) entra com as migrations (ver
  [`roadmap.md`](../roadmap.md)).
