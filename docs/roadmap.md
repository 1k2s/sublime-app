# Roadmap

Estado do projeto, próximos passos e o que ainda não existe. É o único documento
com status: [`architecture.md`](architecture.md) e
[`domain-model.md`](domain-model.md) descrevem só o que está implementado, e a
feature em andamento fica na spec dela, em [`specs/`](specs/).

## Fases

- **Fase 1:** cadastros, contratos e lançamento de atendimentos (repasse ao
  prestador e saldo de sessões).
- **Fase 2:** `Payment` e saldo financeiro do paciente (visão da clínica).
- **Fase 3:** app de consulta de saldo para o paciente.

## Estado atual (Fase 1 — atualizado em 2026-10-21)

| Módulo | Situação |
|---|---|
| `patient` | implementado: incl. origem do paciente (`referringProvider`) e soft delete com 409 |
| `user` | implementado: soft delete, e-mail único (409) |
| `provider` | implementado: dois percentuais (padrão 40/45), usuário ativo obrigatório |
| `pricing` | implementado: grupos, técnicas, planos (`PlanType`, `termWeeks`, de-para), preços com reajuste historizado |
| `contract` | implementado: cabeçalho + `ContractItem`, vigência calculada (`SessionFrequency`), aditivos, opções do plano do Grupo, endpoints de apoio, beneficiário de no máximo um contrato vigente |
| `consultation` | implementado: lançamento com contratos vinculados e escolha livre, resolução de preço (casos 1 a 3), `pricingSource`, comissão pela origem, repasse `HALF_UP`, saldo pela cadeia de versões, soft delete sem `PUT`, endpoints de apoio (`eligible-contracts`, `session-balance`) |

- **A aplicação sobe** (verificado em 2026-10-21, com o banco zerado): o Spring
  aceitou os métodos derivados e os `@Query`, e o schema gerado confere com as
  entidades. **Nenhum endpoint foi chamado ainda**, em nenhum módulo.
- **Outro computador ou banco antigo:** zerar o banco antes de rodar
  (`docker compose down -v`; ver [`development.md`](development.md)). As mudanças de
  outubro em `plan`, `contract`, `contract_item`, `provider`, `patient` e
  `consultation` removem colunas e mudam constraints.
- **Testes:** decidido testar pela **interface gráfica** (não por Swagger nem por
  testes automatizados por enquanto).

## Próximos passos

1. **Interface gráfica** (ReactJS) e testes por ela. Começar com uma spec em
   [`specs/`](specs/README.md).
2. **Migrations do Flyway** e o que depende delas (ver "Pendências técnicas").

## Pendências técnicas

- **Reativação de paciente, prestador e técnica** (endpoint), antes ou junto da
  interface gráfica. O lançamento de atendimento recusa registro inativo (400) e a
  regra é "o admin reativa antes de lançar", mas não existe caminho pela API:
  `PatientEntity.activate()` e `ProviderEntity.activate()` existem sem service nem
  endpoint, e a `TechniqueEntity` não tem o método. *Em aberto:* se reativar um
  registro já ativo dá 409 (espelhando o `deactivate`).
- **Migrations (Flyway):**
  - `CHECK` do exclusive arc em `contract_item` e `consultation`
    (`(session_duration_price_id IS NULL) <> (session_frequency_price_id IS NULL)`).
    Até lá, garantido no código;
  - unique de "uma linha de preço vigente": coluna gerada
    `current_flag = IF(valid_to IS NULL, 1, NULL) STORED` +
    `UNIQUE (pricing_group_id, plan_id, duration_minutes|weekly_frequency, current_flag)`.
    Resolve também a concorrência no cadastro de preço (duas requisições
    simultâneas criando duas linhas vigentes). Por causa dela, o reajuste fecha a
    linha antiga com `saveAndFlush` antes do INSERT da nova (o Hibernate executa
    INSERTs antes de UPDATEs no flush);
  - trocar `ddl-auto` para `validate` e zerar o banco local;
  - dados reais do catálogo (grupos, técnicas, planos, preços) podem virar
    migration em `src/main/resources/db/migration/`.

## Melhorias futuras (anotadas, sem prioridade)

- **Autenticação:** senha em texto puro e `SecurityConfig` com `permitAll`. Entram
  hash de senha, login e perfis (`ADMIN`/`PROVIDER`) com visibilidade por perfil;
  junto, o `launchedByUserId` no `Consultation` (quem lançou) e a regra de
  permissão do lançamento.
- `PUT` de grupo, técnica e plano (renomear).
- Reajuste de preço agendado (`validFrom` futuro).
- Categorias de prestador (Padrão, Estagiário...) com o par de percentuais, se
  surgirem muitas faixas.
- Lista explícita de frequências permitidas por plano, se a clínica quiser
  bloquear alguma combinação abaixo do teto de 52 semanas.
- Código de erro nas respostas (ex: `PATIENT_HAS_CURRENT_CONTRACT`).
- Value object `Cpf` (com dígitos verificadores).
- Testes automatizados.

## Não modelado ainda

- **`Payment`** (Fase 2): controle financeiro do paciente (pagamentos realizados ×
  valor consumido nos atendimentos lançados), base da visão da clínica. Também é a
  base do "em dia" do Pilates em Grupo (parcela do mês paga), que não usa saldo de
  sessões.
- **Saldo do paciente:** módulo que reúne, por paciente, os **contratos ativos**
  com o saldo de sessões de cada item (ex: "RPG: 22 sessões, 12 usadas;
  Quiropraxia: 12 sessões, 6 usadas") e um **valor em R$** na conta do paciente,
  separado do saldo de sessões:
  - **Origem:** ao **encerrar** um contrato, a clínica informa se o valor restante
    já foi pago ou se vira saldo do paciente (positivo = crédito; negativo =
    débito). O contrato fica inativo e o valor passa a existir só no saldo. Também
    recebe as sessões restantes de item removido por aditivo e o valor de
    atendimentos pelo de-para (caso 2 da "Resolução de preço" em `Consultation`, no
    [`domain-model.md`](domain-model.md), que grava o contrato de origem).
  - Cada lançamento de saldo tem FK do **contrato de origem**.
  - **Destino:** a clínica escolhe entre abater em sessões de um novo contrato
    (renovação), **estornar** ou deixar congelado. Quando resolvido, marca como
    **quitado** (débito: quando o paciente paga).
  - O contrato ganhará uma operação de **encerramento** (ex: `finish(...)`),
    diferente do soft delete (`deactivate`), porque tem efeito financeiro.
  - *Em aberto:* como o valor restante é calculado (sessões restantes × valor
    travado do item?); e o destino do valor do atendimento avulso de **paciente sem
    contrato** (caso 3 do lançamento): pago na hora ou lançado como débito no saldo.
- **Pagamento do repasse ao prestador** (fechamento do período): quando existir,
  atendimento de período já pago não pode ser inativado, e pode entrar um limite de
  retroatividade para lançamentos. Junto, o total do repasse por prestador no
  período (hoje a listagem de atendimentos só filtra; não soma).
- **App de consulta de saldo do paciente** (Fase 3): camada de consumo somente
  leitura sobre `consultation` e `payment`, sem entidades novas.
- **Agendamento** (sem fase definida): hoje a clínica usa outro software para a
  agenda, onde ficam também as desmarcações sem cobrança e a taxa de desmarcação.
  Se um dia for integrado ao sistema, os cancelamentos sem cobrança voltam a ter
  onde ser registrados.
