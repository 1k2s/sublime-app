# Decisões de arquitetura e de domínio (ADRs)

Cada arquivo registra **uma decisão**: o problema, o que foi decidido, as
alternativas descartadas e as consequências aceitas. Os documentos de referência
([`architecture.md`](../architecture.md) e [`domain-model.md`](../domain-model.md))
dizem a regra com um "por quê" de uma linha e apontam para o ADR.

## Regras

- **Quando escrever:** a decisão tinha alternativas reais, é difícil de reverter
  (schema, dependência entre módulos, formato de dados) ou alguém provavelmente vai
  perguntar "por que não fizeram X?". Decisão pequena fica só com uma linha na
  referência.
- **ADR aceito não é editado.** Se a decisão mudar, cria-se um ADR novo que o
  substitui, e o antigo recebe só o status "Substituído por NNNN". Correção de
  digitação ou de link é permitida.
- **Numeração sequencial**, nunca reaproveitada: `NNNN-titulo-em-kebab-case.md`.
- Decisões de uma feature em andamento ficam na spec dela ([`../specs/`](../specs/))
  e viram ADR quando a feature é concluída.

## Modelo

```markdown
# NNNN — Título da decisão

- **Status:** aceito | substituído por NNNN
- **Data:** AAAA-MM-DD

## Contexto
O problema e as restrições que levaram à decisão.

## Decisão
O que foi decidido.

## Alternativas descartadas
- Alternativa: por que não.

## Consequências
O que muda, o que fica mais fácil e o que foi aceito como custo.
```

## Índice

Os ADRs 0001 a 0013 foram registrados retroativamente em 2026-10-20, a partir das
discussões que estavam no `domain-model.md` e no `architecture.md`.

| ADR | Decisão | Status |
|---|---|---|
| [0001](0001-monolito-modular.md) | Monolito modular com dependência unidirecional | aceito |
| [0002](0002-value-object-de-um-valor.md) | Value object de um valor: `record` + `AttributeConverter`, sem arredondar | aceito |
| [0003](0003-preco-historizado.md) | Preço historizado por `validFrom`/`validTo` exclusivo | aceito |
| [0004](0004-grupo-de-preco-e-duas-tabelas.md) | Preço no grupo, com duas tabelas pelo `pricingModel` | aceito |
| [0005](0005-tipo-do-plano.md) | Tipo do plano (`PlanType`) | aceito |
| [0006](0006-prazo-do-grupo-em-semanas.md) | Prazo dos planos do Grupo em semanas (`termWeeks`) | aceito |
| [0007](0007-de-para-de-planos.md) | De-para de planos nos dois lados | aceito |
| [0008](0008-contrato-com-itens.md) | Contrato como agregado com um item por grupo | aceito (em parte substituído por 0040: item por duração no contrato de mensalidade) |
| [0009](0009-vigencia-calculada.md) | Vigência calculada e `SessionFrequency` | aceito (em parte substituído por 0040: a vigência do contrato de mensalidade) |
| [0010](0010-plano-do-item-do-grupo.md) | Opções do plano do item do Grupo | aceito |
| [0011](0011-contrato-versionado-por-aditivos.md) | Contrato versionado por aditivos | aceito (em parte substituído por 0039: o número da versão, e por 0040: item por duração no contrato de mensalidade) |
| [0012](0012-comissao-pela-origem-do-paciente.md) | Comissão pela origem do paciente | aceito |
| [0013](0013-prestador-com-usuario.md) | Prestador sempre com usuário | aceito (em parte substituído por 0031: o motivo) |
| [0014](0014-tres-status-cobrados.md) | Só três status de atendimento, todos cobrados | aceito |
| [0015](0015-contrato-do-atendimento.md) | Contrato do atendimento: vínculo na data e escolha livre | aceito |
| [0016](0016-resolucao-de-preco-no-lancamento.md) | Resolução de preço no lançamento pelo sistema | aceito |
| [0017](0017-contrato-e-origem-do-preco-gravados.md) | Contrato e origem do preço gravados no atendimento | aceito |
| [0018](0018-saldo-esgotado-aceita-lancamento.md) | Saldo esgotado aceita o lançamento | aceito |
| [0019](0019-repasse-com-half-up.md) | Repasse arredondado com `HALF_UP` | aceito |
| [0020](0020-lancamento-imutavel.md) | Lançamento de atendimento imutável | aceito |
| [0021](0021-schema-versionado-flyway.md) | Schema versionado pelo Flyway, com `validate`, e enums como `varchar` | aceito (em parte substituído por 0028, até a versão estável, e por 0030: o catálogo) |
| [0022](0022-monorepo.md) | Monorepo: backend e frontend no mesmo repositório | aceito |
| [0023](0023-autenticacao-jwt-resource-server.md) | Autenticação por JWT próprio, validado pelo resource server do Spring | aceito (em parte substituído por 0027: o primeiro admin) |
| [0024](0024-sessao-frontend-localstorage.md) | Sessão do frontend no `localStorage`, encerrada pela `expiresAt` e pelo 401 | aceito |
| [0025](0025-primeiro-acesso-com-senha-provisoria.md) | Primeiro acesso com senha provisória no próprio hash e troca automática no login | aceito (em parte substituído por 0029: como o estado é guardado) |
| [0026](0026-acesso-por-perfil-centralizado.md) | Autorização por perfil: tabela única no `SecurityConfig`, com o `ADMIN` como padrão | aceito |
| [0027](0027-admin-padrao-protegido.md) | Admin padrão protegido por uma marca no usuário | aceito |
| [0028](0028-migrations-editaveis-ate-versao-estavel.md) | Migrations editáveis até a versão estável | aceito |
| [0029](0029-senha-provisoria-por-coluna.md) | Senha provisória marcada por uma coluna, não por prefixo no hash | aceito |
| [0030](0030-seed-do-catalogo-por-migration.md) | Seed do catálogo de preços por migration | aceito |
| [0031](0031-prestador-com-usuario-revisto.md) | Prestador continua sempre com usuário, com o admin lançando por ele | aceito |
| [0032](0032-valor-e-parcelas-do-contrato.md) | Valor e parcelas do contrato calculados pelas linhas travadas, só o número de parcelas gravado | aceito (em parte substituído por 0037: as parcelas no aditivo, por 0041: as aulas do Grupo, e por 0046: o valor parcelado gravado) |
| [0033](0033-lancamento-retroativo-com-liberacao.md) | Lançamento só de hoje para o prestador, com o retroativo liberado pelo admin | aceito |
| [0034](0034-hoje-no-fuso-da-clinica.md) | "Hoje" no fuso da clínica, fixado na inicialização | aceito (em parte substituído por 0036: onde o fuso é aplicado) |
| [0035](0035-sessao-sincronizada-entre-abas.md) | Uma sessão por navegador, com as abas sincronizadas pelo evento `storage` | aceito |
| [0036](0036-testes-de-integracao-com-testcontainers.md) | Testes de integração com o MySQL do Testcontainers | aceito |
| [0037](0037-parcelas-escolhidas-no-aditivo.md) | Parcelas escolhidas em todo aditivo | aceito (em parte substituído por 0047: o que as parcelas dividem e o máximo) |
| [0038](0038-previa-do-aditivo.md) | Prévia do aditivo num endpoint e num pedido próprios | aceito |
| [0039](0039-lista-pela-versao-atual.md) | Lista pela versão atual, com o número do aditivo e a data gravados | aceito |
| [0040](0040-item-por-duracao-em-contrato-de-mensalidade.md) | Item por duração em contrato de mensalidade, pelo pacote equivalente | aceito |
| [0041](0041-aulas-do-grupo-pela-vigencia.md) | Aulas do Pilates em Grupo pela vigência do contrato | aceito |
| [0042](0042-conta-do-paciente-pelo-consumo.md) | A conta do paciente pelo consumo, com as parcelas como referência | aceito |
| [0043](0043-pilates-em-grupo-com-duas-contas.md) | Pilates em Grupo com duas contas: o dinheiro e as aulas | aceito |
| [0044](0044-pagamento-bruto-liquido-e-cartao-parcelado.md) | O pagamento: bruto e líquido, e o cartão parcelado como um pagamento só | aceito |
| [0045](0045-vencimento-das-parcelas.md) | O vencimento das parcelas e a data da inativação | aceito |
| [0046](0046-valor-parcelado-gravado-por-versao.md) | O valor parcelado gravado por versão | aceito |
| [0047](0047-parcelas-do-aditivo-sobre-o-que-falta.md) | As parcelas do aditivo sobre o que falta pagar, até os meses que faltam | aceito |
| [0048](0048-paid-amount-provider.md) | O `contract` declara o `PaidAmountProvider`, o `billing` implementa | aceito |
| [0049](0049-status-pelo-saldo-e-credito-transferido.md) | O status do contrato terminado pelo saldo, e o crédito transferido ao contrato novo | aceito |
