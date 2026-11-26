# 0038 — Prévia do aditivo num endpoint e num pedido próprios

- **Status:** aceito
- **Data:** 2026-11-26

## Contexto

O fim do aditivo segue outra regra que o da criação: mantém o fim atual, estende se pedido e
recalcula a partir da data do aditivo quando entra um item por duração
([ADR 0011](0011-contrato-versionado-por-aditivos.md)). As parcelas e as opções do plano do Grupo
dependem desse novo fim. Sem prévia, o admin só veria o resultado depois de salvar, e a correção
seria outro aditivo.

Para o aditivo existia só o `GET /api/contracts/{id}/group-plan-options`, que calculava as opções
do plano do Grupo pela vigência **atual**, enquanto a validação do aditivo usa a **nova**. Nenhuma
tela o usava.

## Decisão

- **`POST /api/contracts/{id}/amendments/preview`**, sem gravar e sem inativar a versão, com um
  **pedido próprio** (`ContractAmendmentPreviewRequestDTO`): só o que muda as contas, todos
  opcionais. São os itens do formulário (podem vir incompletos), o `endDate` (prorrogação) e as
  parcelas (vazio ou acima do máximo = o máximo; a resposta diz as aplicadas).
- **A resposta é a mesma da prévia da criação** (`ContractPreviewResponseDTO`: novo fim, semanas,
  opções do plano do Grupo, valor de cada item, total e parcelas), calculada pela **nova**
  vigência.
- **A mesma conta do aditivo:** o fim sai do `ContractEntity.calculateAmendedEndDate`, público e
  usado pelo `amend` e pela prévia. Segue a regra do aditivo: só a versão ativa (409).
- **Sai o `GET /api/contracts/{id}/group-plan-options`**: a prévia traz as opções certas.

## Alternativas descartadas

- **Reaproveitar a prévia da criação** (`POST /api/contracts/preview`): calcula o fim pelo
  início, e não pela regra do aditivo.
- **Calcular no front:** a regra do fim e das parcelas é do backend (princípio 1 do front, no
  `architecture.md`).
- **Usar o pedido do aditivo na prévia:** ele exige forma de pagamento e itens completos, e daria
  400 com o formulário em preenchimento; forma de pagamento e beneficiário não mudam as contas.

## Consequências

- O [ADR 0010](0010-plano-do-item-do-grupo.md) continua valendo: só o endpoint que devolve as
  opções no aditivo mudou (como o 0032 fez na criação).
- Mudança na API: saiu o `group-plan-options`.
- A tela mostra o "antes → depois" do fim, do total e das parcelas enquanto o admin edita.
