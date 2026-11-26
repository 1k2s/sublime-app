# 0040 — Item por duração em contrato de mensalidade, pelo pacote equivalente

- **Status:** aceito
- **Data:** 2026-11-26

## Contexto

O contrato só com o Pilates em Grupo usa um plano de mensalidade ([ADR 0008](0008-contrato-com-itens.md)),
e o plano nunca muda no aditivo ([ADR 0011](0011-contrato-versionado-por-aditivos.md)). Com isso,
esse contrato não aceitava nenhuma outra técnica: o item por duração usa uma linha do plano do
contrato, e plano de mensalidade não tem linha por duração. Incluir um RPG exigia encerrar o
contrato e fazer outro, o que não é como a clínica trabalha (visto no teste da tela do aditivo).

O de-para já existe nos dois lados justamente para esse cruzamento
([ADR 0007](0007-de-para-de-planos.md)): o lançamento já cobra pelo equivalente a sessão por
duração de quem só tem o Grupo (Trimestral → Evolução).

## Decisão

- **O contrato de plano de mensalidade aceita itens por duração**, na criação e no aditivo. A
  linha do item é do **pacote equivalente**, fixo, pelo de-para (`PlanEntity.planFor`: Mensal →
  Essencial, Trimestral → Evolução, Semestral → Transformação, Anual → Vitalidade, Familiares →
  Familiares). Plano sem equivalente: 400.
- **Vigência:** o maior entre o prazo do plano de mensalidade (`termWeeks`) e o dos itens por
  duração (as sessões do pacote equivalente no ritmo do item). No aditivo, vale a regra que já
  existia: o item incluído conta da data do aditivo e pode estender o fim.
- **O item do Grupo continua obrigatório** no contrato de mensalidade (400 sem ele), espelhando o
  "pacote precisa de pelo menos um item por duração". Para ficar só com itens por duração,
  contrato novo de pacote.
- **No aditivo, o item com a mesma linha da versão anterior é aceito sem reconferir o plano**: o
  de-para pode mudar depois, e um item antigo não pode travar os aditivos seguintes (mesma ideia
  do preço e da técnica mantidos).

## Alternativas descartadas

- **Escolher o pacote numa lista,** como o plano do Grupo no pacote: o de-para já é a regra da
  clínica para esse cruzamento, sem mais uma escolha na tela.
- **Só no aditivo:** a regra é da entidade e vale também num contrato novo.
- **Permitir o contrato de mensalidade sem o item do Grupo:** o plano define o contrato; sem o
  Grupo, o certo é um pacote.
- **Permitir trocar o plano no aditivo:** muda toda a forma de cobrança (o motivo do ADR 0011
  continua valendo).

## Consequências

- Substitui em parte:
  - o [ADR 0008](0008-contrato-com-itens.md): o item por duração usa o plano do contrato **ou,
    num contrato de mensalidade, o equivalente**;
  - o [ADR 0009](0009-vigencia-calculada.md): a vigência do contrato de mensalidade passa a ser o
    maior entre o `termWeeks` e o prazo dos itens por duração;
  - o [ADR 0011](0011-contrato-versionado-por-aditivos.md): "Sem exceção para o plano, nem no
    contrato só com Grupo que precise de item por duração" (o plano continua não mudando; o item
    usa o equivalente).
- Na tela, qualquer plano oferece todas as técnicas, e o item por duração na mensalidade sai com o
  valor do pacote equivalente.
