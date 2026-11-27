# Roadmap

Estado do projeto, próximos passos e o que ainda não existe. É o único documento
com status: [`architecture.md`](architecture.md) e
[`domain-model.md`](domain-model.md) descrevem só o que está implementado, e a
feature em andamento fica na spec dela, em [`specs/`](specs/).

## Índice <!-- omit from toc -->

- [Fases](#fases)
- [Estado atual (Fase 1)](#estado-atual-fase-1)
- [Próximos passos](#próximos-passos)
- [Pendências técnicas](#pendências-técnicas)
- [Melhorias futuras (anotadas, sem prioridade)](#melhorias-futuras-anotadas-sem-prioridade)
- [Não modelado ainda](#não-modelado-ainda)
- [Frontend](#frontend)
  - [Estado atual](#estado-atual)
  - [Próximos passos](#próximos-passos-1)
  - [Pendências](#pendências)
  - [Melhorias (sem prioridade)](#melhorias-sem-prioridade)

## Fases

- **Fase 1:** cadastros, contratos e lançamento de atendimentos (repasse ao
  prestador e saldo de sessões).
- **Fase 2:** `billing`: pagamentos e saldo financeiro do paciente (visão da clínica).
- **Fase 3:** app de consulta de saldo para o paciente.

## Estado atual (Fase 1)

*Atualizado em 2026-11-26.*

| Módulo | Situação |
|---|---|
| `patient` | implementado: incl. origem do paciente (`referringProvider`), soft delete e reativação com 409 |
| `user` | implementado: soft delete e reativação, e-mail único (409), senha com hash BCrypt (mínimo 8), senha opcional na alteração, senha provisória com troca obrigatória no primeiro acesso (coluna `password_change_required`; [ADR 0025](adr/0025-primeiro-acesso-com-senha-provisoria.md), [ADR 0029](adr/0029-senha-provisoria-por-coluna.md)), admin padrão protegido ([ADR 0027](adr/0027-admin-padrao-protegido.md)), Configurações do próprio usuário (`/api/users/me`) |
| `auth` | implementado no backend e no front: login com JWT, rotas protegidas (exceto login, primeiro acesso e Swagger), corte imediato do acesso ao inativar, mudar o perfil ou redefinir a senha, Swagger com "Authorize" ([ADR 0023](adr/0023-autenticacao-jwt-resource-server.md), [ADR 0024](adr/0024-sessao-frontend-localstorage.md)); **autorização por perfil** (2026-11-07): tabela de acesso no `SecurityConfig` com o `ADMIN` como padrão e o `CurrentUser` para as regras de dados ([ADR 0026](adr/0026-acesso-por-perfil-centralizado.md)) |
| `provider` | implementado: dois percentuais (padrão 40/45), usuário ativo obrigatório na criação e na reativação ([ADR 0031](adr/0031-prestador-com-usuario-revisto.md)) |
| `pricing` | implementado: grupos, técnicas (com reativação), planos (`PlanType`, `termWeeks`, de-para), preços com reajuste historizado; **tabela e histórico por grupo** (2026-11-17: `GET /api/pricing-groups/{id}/prices` e `.../prices/history`; o `validTo` entra na resposta das linhas de preço); **status da linha de preço** (2026-11-18: `PriceStatus` Vigente, Vencido ou Encerrado, resolvido pelo backend em `status`; ver "Status da linha de preço" no [`domain-model.md`](domain-model.md)) |
| `contract` | implementado: cabeçalho + `ContractItem`, vigência calculada (`SessionFrequency`), aditivos, opções do plano do Grupo, endpoints de apoio, beneficiário de no máximo um contrato vigente e diferente do titular, `current` na resposta (vigente hoje), **valor total e parcelas** (2026-11-12; coluna `installments`, prévia `POST /api/contracts/preview`; [ADR 0032](adr/0032-valor-e-parcelas-do-contrato.md)); **aditivo completo** (2026-11-26): prévia do aditivo, parcelas escolhidas em todo aditivo, aditivo sem alteração recusado, lista só com a versão atual e o histórico em `/versions`, número do aditivo e data gravados, item por duração em contrato de mensalidade ([ADR 0037](adr/0037-parcelas-escolhidas-no-aditivo.md) a [ADR 0040](adr/0040-item-por-duracao-em-contrato-de-mensalidade.md)) |
| `consultation` | implementado: lançamento com contratos vinculados e escolha livre, resolução de preço (casos 1 a 3), `pricingSource`, comissão pela origem, repasse `HALF_UP`, saldo pela cadeia de versões, soft delete sem `PUT`, endpoints de apoio (`launch-options` com as durações válidas, `session-balance`, `patients`, `recent-patients`, `day-summary`); prestador do login para o `PROVIDER`, que só vê e inativa os próprios lançamentos; `launchedByUserId`; **o `PROVIDER` só lança e desfaz com a data de hoje**, e o retroativo usa a liberação do admin (2026-11-13; tabela `retroactive_launch_grant`, no `provider`; [ADR 0033](adr/0033-lancamento-retroativo-com-liberacao.md)); "hoje" no fuso da clínica ([ADR 0034](adr/0034-hoje-no-fuso-da-clinica.md)); **rendimento do prestador** (2026-11-26): coluna `launched_at`, `earnings-summary` e `earnings` (só os realizados, pela data do lançamento, com as faltas cobradas à parte e o `byProvider` do admin), filtros `launchedFrom`, `launchedTo` e `status` na listagem e o `day-summary` na mesma regra ([ADR 0042](adr/0042-rendimento-do-prestador.md)) |
| **frontend** | **em andamento**: ver a seção [Frontend](#frontend) |

- **Monorepo** desde 2026-10-29 ([ADR 0022](adr/0022-monorepo.md)): o backend em
  `backend/` e o frontend, importado com o histórico, em `frontend/`.
- **Schema versionado pelo Flyway** (V1 a V4, com o seed do catálogo na V4; ver
  [`development.md`](development.md)), com `ddl-auto=validate`. **Até a versão estável, as
  migrations podem ser editadas** ([ADR 0028](adr/0028-migrations-editaveis-ate-versao-estavel.md)):
  a V1 foi editada em 2026-11-07 (colunas `default_admin`, `password_change_required` e
  `launched_by_user_id`) e em 2026-11-12 (coluna `installments` do contrato), então quem puxar
  zera o banco local (`docker compose down -v`). Editada de novo em 2026-11-13 (tabela
  `retroactive_launch_grant`) e em 2026-11-25 (colunas `amendment_number` e `created_at` do
  contrato; coluna `launched_at` e o índice `idx_consultation_provider_launched` do
  atendimento).
- **Teste manual do fluxo completo pela API** (2026-10-29, comandos manuais, sem script): o
  catálogo, os cadastros, o contrato (incluindo o aditivo), os três casos de
  lançamento, o saldo, a reativação e as constraints do banco funcionaram como
  esperado.
- **Teste manual da autenticação pela API** (2026-11-05): login, tokens inválidos e
  vencidos, senha com e sem troca, corte imediato do acesso, primeiro admin, Swagger e CORS
  funcionaram como esperado.
- **Teste manual da autenticação pela interface** (2026-11-06): login de cada perfil, senha
  errada, sessão vencida, logout seguido de outro login e usuário inativado durante o uso
  funcionaram como esperado.
- **Teste manual da autorização por perfil e do primeiro acesso** (2026-11-07, banco
  zerado): 55 casos pela API (acesso de cada perfil, lançamento com e sem `providerId`,
  restrição de dados, admin padrão, `/me`, reativações, primeiro acesso e redefinição de
  senha) e 20 pela interface (menu e rotas por papel, primeiro acesso, Configurações)
  funcionaram como esperado. Também o `AdminInitializer` sem admin padrão: com
  `ADMIN_PASSWORD` inválida, a aplicação não sobe e diz o motivo; com `ADMIN_EMAIL` já
  usado por outro usuário, sobe só com um aviso no log, sem criar ninguém.
- **Variáveis locais:** a aplicação precisa do `backend/.env` (ver "Variáveis de ambiente"
  no [`development.md`](development.md)); sem `JWT_SECRET`, não sobe.
- **Testes automatizados no backend** (2026-11-13): unidade do `Percentage` e do
  `ProviderEntity` e a infraestrutura de integração com o MySQL do Testcontainers
  ([ADR 0036](adr/0036-testes-de-integracao-com-testcontainers.md)), com o primeiro teste de
  integração (`AuthorizationIntegrationTest`: 401 sem token e com token adulterado, rotas por
  perfil, login recusado, primeiro acesso e corte do acesso do usuário inativado). O resto,
  ainda manual (ver "Próximos passos").

## Próximos passos

1. **Telas funcionais no front** (decidido em 2026-11-11, antes dos testes): o backend está
   pronto, mas o sistema ainda não funciona de ponta a ponta (contratos e lançamento usam
   mocks e o modelo antigo). **Uma branch e uma spec por tela**, cada uma com o próprio PR,
   na ordem de dependência (as telas de Usuários, Prestadores, Contratos, Lançamento e Dashboard
   já estão prontas; o Lançamento em 2026-11-13, com o retroativo por liberação do admin; o
   aditivo do contrato e o Dashboard em 2026-11-26):
   1. **Cadastro do catálogo** (spec em [`specs/cadastro-do-catalogo.md`](specs/cadastro-do-catalogo.md),
      2026-11-24): renomear técnica, grupo e plano; inativar e reativar grupo e plano; telas de
      cadastro; as técnicas de cada grupo na tabela de Preços. Hoje a clínica só mexe em preço
      pela tela. **Aguardando** as decisões em aberto da spec e as prévias.
2. **Testes automatizados no backend** (decidido em 2026-11-07; pausado em 2026-11-11 e
   **retomado** em 2026-11-13, com a infraestrutura): cada feature exige dezenas de casos manuais, refeitos a
   cada merge da `dev`, e as regras mais arriscadas são financeiras (resolução de preço,
   comissão, repasse, saldo).
   - **Feito** (2026-11-13): unidade do `Percentage` e do `ProviderEntity` (as 7 regras);
     infraestrutura de integração (`@IntegrationTest`, [ADR 0036](adr/0036-testes-de-integracao-com-testcontainers.md));
     e uma amostra da autorização por perfil (`AuthorizationIntegrationTest`). Em 2026-11-18,
     unidade do `PriceStatus` (Vigente, Vencido e Encerrado, com o limite dos 12 meses). Em
     2026-11-26, o rendimento: integração (`EarningsIntegrationTest`, 15 casos: a soma nativa no
     MySQL, os períodos, as faltas à parte, o `byProvider` e o acesso por perfil) e unidade da
     data do lançamento (`ConsultationEntityTest`).
   - **Próximo:** testes de unidade das outras entidades com regra de risco (resolução de
     preço, comissão no lançamento, saldo, vigência e parcelas do contrato).
   - Depois, mais **testes de integração**: o lançamento (os três casos de preço, o
     retroativo com liberação, o `PROVIDER` só com os próprios dados) e o resto da
     autorização. Os 55 casos manuais da autorização são a base.
   - Opcional, durante as telas: ao fechar cada uma, escrever os testes de unidade das
     regras que ela usa.
3. **CI no GitHub Actions**, agora que há testes: a cada PR, `./mvnw test` no backend (o
   runner do GitHub já tem o Docker, que o Testcontainers usa), build e lint do front e
   `node scripts/check-docs.mjs`, com o resultado no PR.
   CD (deploy automático) fica para quando houver a versão 1 no ar.

## Pendências técnicas

- **Antes da versão 1 no ar:** revisar e consolidar as migrations e voltar a regra de
  nunca editá-las ([ADR 0028](adr/0028-migrations-editaveis-ate-versao-estavel.md)); zerar
  o banco de produção.
- **Antes da versão 1 no ar: versão do MySQL** (notado na revisão dos testes, 2026-11-13). O
  projeto usa o MySQL 8.0, que chegou ao fim do suporte em abril de 2026. Avaliar a migração
  para o 8.4 (LTS) antes de subir o banco de produção: rodar as migrations e a suíte de testes
  na versão nova (mudar a imagem no `docker-compose.yml` e no `TestcontainersConfiguration`).
- **`equals` e `hashCode` das entidades** (revisão dos testes, 2026-11-17). As 12 entidades
  usam `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` só pelo `id`: duas entidades novas
  (com `id` nulo) contam como iguais, num `HashSet` ou num `equals`. O `isSameAs` da
  `ProviderEntity` já compara o próprio objeto quando não há `id`, mas o `equals` não. Padrão do
  projeto todo (coleções e Hibernate): numa branch e com um ADR próprios.
- **Desligar o open-in-view** (`spring.jpa.open-in-view=false`; revisão dos testes,
  2026-11-17). Ligado (o padrão do Spring), ele mantém a sessão do Hibernate aberta durante toda
  a requisição: um lazy loading fora do service funciona sem aviso, inclusive nos testes.
  Desligar é o recomendado, mas muda todos os endpoints; fazer depois dos testes de integração
  do lançamento e dos cadastros.
- **N+1 nas listagens de contratos e pacientes** (revisão de 2026-11-11; numa branch
  própria, junto com o item seguinte). O `ContractRepository` não sobrescreve o `findAll` com
  `@EntityGraph`: o `GET /api/contracts` faz uma consulta de itens por contrato, mais uma por
  linha de preço, técnica, grupo e plano distintos. O `GET /api/patients` faz uma por
  prestador de origem distinto. É desvio do padrão "Consulta com relação" do
  [`architecture.md`](architecture.md#padrão-de-construção-todos-os-módulos-implementados-seguem);
  seguir o precedente do `PlanRepository.findAll`.
- **Buscas de preço vigente fora do `pricing`** (revisão de 2026-11-11; na mesma branch do
  N+1). O `ContractService` e o `ConsultationService` montam as buscas direto nos
  repositórios do `pricing` (linha vigente por grupo + plano + eixo + `validTo` nulo, o único
  Avulso ativo, as mensalidades por prazo; desde 2026-11-13, também as durações com preço das
  opções do lançamento, no `findLaunchOptions`), contra o "nenhum outro módulo deve conhecer a
  fiação interna" do [`architecture.md`](architecture.md#mapa-de-módulos-fase-1). Levar as
  buscas para o `PricingService` antes do reajuste agendado, que muda o que é "vigente".

Frontend: ver a seção [Frontend](#frontend).

## Melhorias futuras (anotadas, sem prioridade)

- Reajuste de preço agendado (`validFrom` futuro).
- Categorias de prestador (Padrão, Estagiário...) com o par de percentuais, se
  surgirem muitas faixas.
- Lista explícita de frequências permitidas por plano, se a clínica quiser
  bloquear alguma combinação abaixo do teto de 52 semanas.
- Código de erro nas respostas (ex: `PATIENT_HAS_CURRENT_CONTRACT`).
- Value object `Cpf` (com dígitos verificadores).
- **Nome do usuário no login:** o `user` da resposta do `POST /api/auth/login` traz só
  `id`, `email` e `role`, e o menu do front mostra o e-mail. Para mostrar um nome, a
  resposta precisa trazê-lo. O `User` não tem nome hoje: decidir entre um campo `name` no
  `User` e o nome do `Provider` vinculado (que não cobre o admin sem prestador).
- **Expiração da senha provisória** (ex: 7 dias, como no AWS Cognito): hoje ela vale até o
  primeiro acesso.

## Não modelado ainda

- **`billing`** (Fase 2; esboço decidido na análise de 2026-11-18): o que é cobrado e o que é
  pago, base da visão da clínica. O nome cobre os dois lados; `Payment` é uma entidade dentro
  dele (precedente de módulo com o nome do assunto: o `pricing`).
  - **A dívida do paciente é o consumo** (atendimentos lançados × valor), comparado com os
    pagamentos: o "controle de atendimento" que a clínica faz hoje à mão. Quem vai mais que o
    contratado termina devendo; quem desmarca com antecedência termina com crédito. O sistema
    já grava o consumo (`Consultation.baseValue`, saldo negativo aceito pelo
    [ADR 0018](adr/0018-saldo-esgotado-aceita-lancamento.md)); falta o pago.
  - **O contrato** trava o preço, define a expectativa de uso (sessões e frequência) e o plano
    de pagamento (as parcelas). As parcelas **não** são a dívida
    ([ADR 0037](adr/0037-parcelas-escolhidas-no-aditivo.md)). Sessão além do contratado vale o
    valor do item do contrato (a linha travada), como o lançamento já faz.
  - **Pilates em Grupo:** mensalidade, sem controle de sessões: a dívida é a parcela do mês
    (o "em dia" é a parcela paga), não o consumo.
  - **Avisos:** quando o consumo passa do contratado ou do que foi pago, para a clínica avisar
    o paciente cedo (antes de o valor virar 2x ou 3x o acordado).
  - **Módulos:** o `billing` fica **acima** do `consultation` no mapa e **lê** o `contract` e o
    `consultation`, sem juntá-los (agregados com regras e ciclos de vida diferentes: versionado
    e imutável). Precedente: o saldo de sessões é calculado no `consultation`, lendo o
    `contract`.
- **Saldo do paciente** (no `billing`): reúne, por paciente, os **contratos ativos**
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
  retroatividade para lançamentos. O quanto cada prestador rendeu no período já existe (o
  `earnings`, ver "Rendimento" em `Consultation`, no [`domain-model.md`](domain-model.md));
  falta o quanto já foi pago.
- **Impressão do contrato para assinatura:** cada versão (a original e cada aditivo) é
  impressa para o paciente assinar (ver "Versionamento (aditivos)" em `Contract`, no
  [`domain-model.md`](domain-model.md)). Hoje não há impressão.
- **App de consulta de saldo do paciente** (Fase 3): camada de consumo somente
  leitura sobre `consultation` e `billing`, sem entidades novas.
- **Agendamento** (sem fase definida): hoje a clínica usa outro software para a
  agenda, onde ficam também as desmarcações sem cobrança e a taxa de desmarcação.
  Se um dia for integrado ao sistema, os cancelamentos sem cobrança voltam a ter
  onde ser registrados.

## Frontend

Convenções na seção "Frontend" do [`architecture.md`](architecture.md).

### Estado atual

*Atualizado em 2026-11-26; a revisar no inventário.*

Levantado pelos arquivos de `frontend/src/`; **o comportamento de cada tela e se as
chamadas batem com a API real ainda não foram conferidos.**

| Módulo | O que existe |
|---|---|
| `auth` | **concluído e testado pela tela** (2026-11-07): login real, primeiro acesso (a tela troca sozinha para a definição da senha), sessão no `localStorage` que expira pela `expiresAt` e cai num 401, cache limpo ao sair, **abas sincronizadas** (2026-11-15: uma sessão por navegador, e as abas abertas acompanham quem entra ou sai; [ADR 0035](adr/0035-sessao-sincronizada-entre-abas.md); **testado pela tela** com duas abas: sair, entrar com outro perfil, trocar o e-mail e abrir aba nova), tela inicial por perfil (`HomeRedirect`), **rotas e menu por papel** (`ProtectedRoute` com `roles`), mensagem da API no `error.message` (interceptor do `apiClient`). Ver "Rotas e sessão" no [`architecture.md`](architecture.md) |
| `patient` | listagem com filtros e busca, modal de detalhes, cadastro em wizard (dados pessoais com a origem do paciente, endereço com ViaCEP, confirmação), ligado à API real (PR #33) |
| `pricing` | **Tabela de Preços** (`/pricing`, só `ADMIN`; 2026-11-17), no verde da sidebar: grupo escolhido no `RichSelect`, uma linha por versão do plano e uma duração (ou frequência) por coluna, a partir do histórico do grupo (`GET /api/pricing-groups/{id}/prices/history`); coluna **Status** com "Vigente", "Vencido" (linha aberta há mais de 12 meses sem reajuste, resolvido pelo backend em `status`) ou "Encerrado" (linha encerrada por reajuste; as encerradas no mesmo dia formam uma linha; o período fica no detalhe); filtro Todos / Vigentes / Vencidos / Encerrados (abre em Vigentes); tabela no `DataTable` das listagens, com o carregando da marca (`ListMessage`); detalhe pelo olho e **alteração dos valores** das versões abertas (vigente ou vencida; um `POST` por valor alterado); "Novo Preço / Reajustar" para qualquer combinação (ex: plano ou duração nova). Conferida por build e lint; **não testada pela tela** |
| `consultation` | tela de Lançamento (`/consultations`, os dois perfis; 2026-11-13), ligada à API: **barra fixa usada toda pelo teclado** (`Combobox`: paciente, técnica, contrato só com dois, duração com as válidas do backend, status; o Enter pula de campo, e do status leva ao Lançar), **um atendimento por vez**, com aviso de sucesso; pacientes recentes como **sugestão** (técnica e duração em azul); só o aviso de saldo (≤ 2 ou esgotado); data **Hoje** ou **Retroativo** (com liberação do admin); lista de hoje com rolagem própria e "Desfazer" com confirmação; rendimento do dia do backend, com as faltas cobradas à parte (2026-11-26); o `ADMIN` escolhe o prestador. **Testada pela API e pela tela** (2026-11-13). **Tela de Dashboard** (`/dashboard`, os dois perfis; 2026-11-26): abas Resumo e Panorama executivo (slide entre elas), barra de período (Hoje, Esta semana, Este mês, Personalizado), para o `ADMIN` a lista "Rendimento por prestador" na altura dos números, três cartões (rendimento, faltas cobradas, repasse total), a lista de atendimentos pela data do lançamento (tabela sempre na tela, filtro de status) e o anel da composição do repasse com o ECharts ([ADR 0043](adr/0043-graficos-com-echarts.md)). Conferida por build e lint; **não testada pela tela** (ver "Pendências") |
| `contract` | tela de Contratos (`/contracts`; 2026-11-13), ligada à API, no modelo real (um item por grupo de preço): listagem com o status Vigente, Vencido ou Inativo (`current` da API), detalhe com o valor total e as parcelas, inativar, e cadastro em **modal grande com rodapé fixo** (plano no `RichSelect` com o alternador Individual \| Familiar, itens em linhas, prévia `POST /api/contracts/preview` com o fim, os valores e as parcelas); erros por campo da API. **Aditivo** (2026-11-26): "Editar" no detalhe, formulário com o antes → depois do fim, do total e das parcelas (prévia do aditivo), saldo de sessões e "Novo" nos itens, revisão com as alterações antes de salvar; a lista com um contrato por linha (coluna "Versão") e o detalhe com abas e o histórico das versões numa linha do tempo, com as mudanças marcadas. **Testado pela API e pela tela** (2026-11-25). Primeiro módulo com `display/`, `utils/` e `hooks/` (ver "Dentro de cada módulo" no [`architecture.md`](architecture.md)). **Testada pela API e pela tela** (2026-11-12 e 2026-11-13) |
| `provider` | tela de Prestadores (`/providers`, só `ADMIN`; 2026-11-11): listagem, detalhes (com o status do usuário vinculado) e cadastro/edição em **modal**. O cadastro **vincula um usuário existente** (ativo e sem prestador); percentual vazio usa o padrão do backend; erros por campo da API. No detalhe, a **liberação de lançamento retroativo** (2026-11-13, testada pela tela). Ligada à API; os casos de teste do cadastro ainda não foram rodados |
| `user` | tela de Configurações (`/settings`, os dois perfis): troca do e-mail e da senha com a senha atual; camada `api/` (usuários e `/me`). **Tela de Usuários** (`/users`, só `ADMIN`; 2026-11-10): listagem com busca e filtros, cadastro e alteração com senha provisória, inativar e reativar, admin padrão protegido. Ligada à API e **testada pela tela** (2026-11-11) |
| `app/`, `shared/` | **casca do app** (2026-11-17): menu lateral próprio que empurra o conteúdo, painel claro do conteúdo com animação de entrada, largura única das telas e fonte da marca (ver "Estilo" no [`architecture.md`](architecture.md)); **testada pela tela** (2026-11-17). Em 2026-11-26, o menu claro, que abre e recolhe pelo clique (botão na borda e logo), com o nome do item num balão no recolhido. **Tabela única das listagens** (`DataTable`, 2026-11-26): cabeçalho fixo e rolagem dentro da tabela, clique na linha e a linha aberta marcada, nas cinco listagens, no Lançamento e no Dashboard. Router com rotas `lazy`, `apiClient`, utilitários (`money`, `date`, `masks`, `alert`, `viacep`) e componentes de `shared/components/` |

- **Telas de listagem padronizadas** (2026-11-11): Usuários, Pacientes, Prestadores e
  Contratos seguem o mesmo padrão (filtro na page, modal pelo `id`, permissão só no router,
  `ListMessage` com os quatro estados, um hook por endpoint para inativar e reativar). Ver
  "Telas de listagem" no [`architecture.md`](architecture.md). **Teste manual pela tela**
  (2026-11-11), depois da padronização: filtros, inativar e reativar com o modal aberto,
  mensagens da tabela, cadastro e edição nas quatro telas, busca de paciente no contrato e
  no lançamento, e Configurações; tudo funcionou como esperado.
- **Limpeza do front** (2026-11-14, branch `chore/frontend-cleanup`): a mensagem da API no
  modal de preço, os exports sem uso removidos (o `isValidCpf` também: o backend não confere os
  dígitos verificadores, e o front ficaria mais rígido que a API), o `PageLoader` e o
  `RouteError` em CSS Modules, o `frontend/.env` fora do git com o modelo `.env.example`, e o
  `apiClient` que falha ao carregar sem a `VITE_API_URL`. Conferido por build e lint; **não
  testado pela tela**.
- **`npm run build` funciona** desde a atualização de 2026-11-05 (as correções do PR #13
  do repositório antigo resolveram os arquivos cortados). `npm run lint`: sem erros e sem
  avisos (2026-11-07).
- Monorepo desde 2026-10-29: o front foi importado com o histórico da `dev` do repositório
  antigo e atualizado em 2026-11-05 com os PRs #13 (correções) e #14 (tela de contratos).
  O repositório antigo está arquivado no GitHub.

### Próximos passos

1. **Telas funcionais**, uma branch e uma spec por tela ([`specs/`](specs/README.md)), na
   ordem de cadastro (cada uma depende dos dados da anterior): o cadastro do catálogo;
   Prestadores, Contratos (com o aditivo), Lançamento e Dashboard já estão prontas. O que cada
   uma fecha e as decisões já tomadas: item 1 dos [Próximos passos](#próximos-passos) do
   projeto.
2. **Inventário** das telas que não entram no item 1: o que cada uma faz e se as chamadas
   batem com o contrato real da API (Swagger). Atualizar esta seção com o resultado. Preços já
   foi refeita e conferida contra o backend (2026-11-17).
3. **Nova identidade visual do portal** (levantado em 2026-11-14). **Feito:** a casca do app e o
   menu lateral, com o logo da marca (2026-11-17); o menu claro, o fundo claro do conteúdo, a
   tabela única das listagens, as etiquetas e as cores dos estados em tokens (2026-11-26).
   **Falta:** a melhoria **"Cores em variáveis CSS em todos os componentes"** e a conferência do
   **"Visual global em cada tela"**, com as prévias antes do código. A `Topbar` entra só se a
   identidade tiver barra superior.

Checklist da Fase 1 no front (herdado do guia original):
- [x] Login funcionando e rotas protegidas.
- [ ] CRUD de `Patient` com endereço e ViaCEP.
- [ ] Catálogo de `pricing` consultável.
- [x] Cadastro de `provider` e `contract`.
- [x] Fluxo completo de lançamento.
- [x] Histórico de atendimentos com filtro por período (o Dashboard, 2026-11-26).
- [ ] Valores monetários enviados como texto, formatados só na exibição, sem cálculo no front.
- [x] `npm run lint` sem erros e `npm run build` gerando o bundle.

### Pendências

- **Dashboard: teste manual pela tela** (2026-11-26; a feature foi concluída por build, lint e
  testes do backend). Com os dois perfis e um banco com vários prestadores, retroativos e um
  lançamento desfeito: os números e a lista do `PROVIDER` (só os dele) e do `ADMIN` (todos e
  cada prestador pela lista ao lado, que rola com muitos prestadores e tem um ativo zerado); os
  quatro períodos e o Personalizado com a data inicial depois da final (a mensagem do backend);
  o retroativo (as duas datas diferentes) e o desfeito ("Desfeito", repasse riscado, fora do
  total) na lista; o filtro de status; a tabela rolando por dentro com o cabeçalho fixo; o slide
  entre as abas e pelo teclado; o anel (entrada, hover na fatia e na legenda, troca de período,
  redimensionar ao abrir e fechar o menu, período sem repasse); o rendimento do dia no
  Lançamento batendo com o Hoje do Dashboard; e, no `npm run build`, o ECharts só no chunk do
  Panorama.
- **Paginação da listagem de atendimentos** (levantado no dashboard, 2026-11-26): o
  `GET /api/consultations` devolve o período inteiro; conferir se o mês de todos os prestadores
  precisa de paginação quando a base crescer.
- **9 vulnerabilidades nas dependências** (3 moderadas, 6 altas; `npm audit` de 2026-11-17).
  Primeiro as que rodam no app: `zod` < 4.6.6 e `follow-redirects` (do `axios`); as outras são
  das ferramentas de build (`vite`, `picomatch`, `minimatch`). Numa branch `chore/` própria,
  com teste do app inteiro: o `npm audit fix` sobe o `zod`, o `axios` e o `vite`.
- *Em aberto:* **CSS aninhado** (o nativo, com `&`) **como padrão do projeto**: hoje só no
  `AppLayout` e no `AppSidebar`.
- *Em aberto:* **fonte da marca no título do login** (hoje Georgia).
- *Em aberto:* **boleto no cadastro de contrato.** O formulário oferece Pix, cartão de crédito,
  cartão de débito e dinheiro; o enum `PaymentMethod` também tem `BANK_SLIP` (um contrato que
  venha com ele aparece como "Boleto"). Decidido deixar assim por enquanto (2026-11-11).
- **Organização dos módulos em `display/`, `utils/` e `hooks/`** (decidida em 2026-11-13, feita
  no `contract`, no `consultation` e no `provider`; ver "Dentro de cada módulo" no
  [`architecture.md`](architecture.md)): levar para cada módulo quando ele for mexido. Arquivos
  ainda na raiz: `roles.js` (auth), `filterPatients.js` e `filterUsers.js`. Onde um componente tiver muita conta além do layout, ela vai para um hook em
  `hooks/`.
- **Visual global em cada tela** (fundo e bordas frias, cards com sombra, menu e título; feito
  em 2026-11-12 e valendo para todas as telas): as listagens ficaram iguais à de Contratos em
  2026-11-26 (a mesma `DataTable`); falta conferir o resto de Pacientes, Usuários e Prestadores
  (filtros, modais) e Configurações, quando cada uma for mexida.
- **Tela de Preços: o que falta** (2026-11-17): o teste pela tela (tabela dos quatro grupos,
  filtro por status com uma linha vencida, detalhe, alteração de um e de dois valores, valor vazio na duração sem preço, erro da API no
  segundo valor); o `PriceFormModal` ("Novo Preço / Reajustar") ainda mostra o erro da API dentro
  do formulário, fora do padrão (`showError` na page e erros por campo), e repete no front a
  regra de quais planos cada grupo aceita (`isPlanAllowed`, cópia do `PlanType.isPricedBy`).
- **Cadastro de grupo, técnica e plano pela tela**: na spec
  [`cadastro-do-catalogo.md`](specs/cadastro-do-catalogo.md) (item 1 dos Próximos passos do
  projeto). Até lá, só pelo Swagger ou pelo seed.
- **Avisos do sistema ao usuário** (notado nos testes de 2026-11-13): quando a sessão cai (401:
  vencida, usuário inativado, perfil alterado), o `signOut` volta para o login sem dizer o
  motivo. Levantar onde cabe um aviso (modal ou o `shared/utils/alert.js`): a sessão
  encerrada e outras ações do sistema.
- **Mensagem de erro que não é da API** (revisão de 2026-11-14): o interceptor do `apiClient`
  só traduz os erros de requisição. Um erro do JavaScript antes de a requisição sair (ex: no
  `mutationFn`) chega à tela em inglês, pelo `error.message`, no `PriceFormModal` e em todo
  `showError(error.message)`. Decidir uma mensagem de reserva em português num lugar só.
- **Arquivos `.env` por modo do Vite** (revisão de 2026-11-14): o `frontend/.gitignore`
  ignora só o `.env` e os `*.local`; um `.env.production` seria versionado. Trocar por `.env*`
  com a exceção `!.env.example`.

### Melhorias (sem prioridade)

- **Outros gráficos no dashboard, se a clínica pedir depois de usar a tela** (2026-11-26): o
  rendimento por dia (barras), os atendimentos por status (as faltas separadas das desmarcações
  fora do prazo) e por técnica (o `techniqueId` existe para esse tipo de relatório). Cada um
  precisa de uma consulta no backend (o front não agrupa nem soma; ver "Gráficos" no
  [`architecture.md`](architecture.md)).
- **Cores em variáveis CSS em todos os componentes.** Hoje o código está misturado: parte
  dos `.module.css` usa `var(--color-*)` e parte usa o hexadecimal direto (uma regra antiga
  do guia, já removida). Padronizar em variáveis, para a paleta mudar num lugar só. **Em
  parte feito** (2026-11-12): o fundo e as bordas (os dois tons bege) passaram para
  `var(--color-bg)` e `var(--color-border)` em todos os `.module.css`. Em 2026-11-26, as tabelas,
  as etiquetas, as molduras, os avisos e os erros também. Ainda com tons escritos direto, entre
  outros: `Button`, `Input`, `Select`, `RichSelect`, `Stepper`, `PatientSearchInput` e
  `TechniqueSearchInput` (os losangos do `BackgroundShapes` têm cores próprias, decorativas).
- **Estado inicial do cadastro de contrato sem itens** (ideia das prévias de 2026-11-12): uma
  área "Nenhum item ainda" com atalhos para as técnicas, no lugar da primeira linha vazia.
- **Buscas no padrão do `Combobox`** (2026-11-13): o `PatientSearchInput` e o
  `TechniqueSearchInput` (Contratos) podem virar `Combobox`, como a barra do Lançamento, e a
  função `normalize` duplicada no `TechniqueSearchInput` e no `TechniqueIcon` passa a usar o
  `normalizeText` (`shared/utils/text.js`).
- **Lançamento, ideias da tela de 2026-11-13:** revogar uma liberação de retroativo antes do
  fim do dia (hoje, ela só acaba às 23:59 ou ao esgotar); um "Corrigir" na lista de hoje, que
  desfaz e devolve os dados à barra.
- **Regras de dependência entre módulos garantidas pelo ESLint**
  (`import/no-restricted-paths` ou `eslint-plugin-boundaries`): o lint falha quando um
  módulo importa quem não deve.
- **Tradução DTO ↔ tela na camada `api/`:** converter a resposta da API para o formato da
  tela (e de volta), para uma mudança de contrato mexer num arquivo só.
- **Tipos do contrato da API:** decidir sobre TypeScript com tipos gerados do OpenAPI do
  backend (`openapi-typescript`), para o editor acusar quando a API mudar.
- **Testes automatizados do front, com o Cypress** (hipótese levantada em 2026-11-14, ainda
  não decidida; depois dos testes do backend). Hoje cada tela é testada à mão, e os casos
  manuais já rodados (os 20 da autorização pela interface, o fluxo do Lançamento pelo teclado,
  o cadastro de contrato) são a base dos primeiros testes. Para a spec, se for adiante:
  - **Contra a API real ou com a API simulada** (`cy.intercept`): a real confere a tela junto
    com o contrato da API (o backend é a fonte da verdade), mas precisa do backend e do MySQL
    no ar e de um banco num estado conhecido a cada execução (hoje só há o
    `docker compose down -v` e o seed da V4); a simulada é rápida e não depende do backend, mas
    não pega mudança de contrato. Provável: poucos fluxos de ponta a ponta contra a API real
    (login, contrato, lançamento) e o resto simulado.
  - **Login sem passar pela tela** em cada teste: `cy.request` ao `POST /api/auth/login` e a
    resposta gravada nas chaves `sublime.auth*` do `localStorage` (`cy.session` guarda entre
    os testes).
  - **Seletores estáveis** (`data-testid` ou papel e rótulo acessíveis), não classes do CSS
    Modules, que mudam de nome no build.
  - **Limite do Cypress: uma aba por teste.** A pendência "Sessão sincronizada entre as abas"
    não se testa com ele; o Playwright testa várias abas e navegadores em paralelo sem custo.
    Comparar os dois na spec antes de instalar.
  - **Funções puras** de `utils/` e `display/` (ex: `filterContracts`, `formatInstallments`)
    se testam mais barato com o Vitest, que usa o mesmo Vite, sem navegador.
  - **CI:** entra no item 3 dos [Próximos passos](#próximos-passos) do projeto; o de ponta a
    ponta exige subir o MySQL e o backend no GitHub Actions.
