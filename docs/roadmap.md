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
- **Fase 2:** `Payment` e saldo financeiro do paciente (visão da clínica).
- **Fase 3:** app de consulta de saldo para o paciente.

## Estado atual (Fase 1)

*Atualizado em 2026-11-07.*

| Módulo | Situação |
|---|---|
| `patient` | implementado: incl. origem do paciente (`referringProvider`), soft delete e reativação com 409 |
| `user` | implementado: soft delete e reativação, e-mail único (409), senha com hash BCrypt (mínimo 8), senha opcional na alteração, senha provisória com troca obrigatória no primeiro acesso (coluna `password_change_required`; [ADR 0025](adr/0025-primeiro-acesso-com-senha-provisoria.md), [ADR 0029](adr/0029-senha-provisoria-por-coluna.md)), admin padrão protegido ([ADR 0027](adr/0027-admin-padrao-protegido.md)), Configurações do próprio usuário (`/api/users/me`) |
| `auth` | implementado no backend e no front: login com JWT, rotas protegidas (exceto login, primeiro acesso e Swagger), corte imediato do acesso ao inativar, mudar o perfil ou redefinir a senha, Swagger com "Authorize" ([ADR 0023](adr/0023-autenticacao-jwt-resource-server.md), [ADR 0024](adr/0024-sessao-frontend-localstorage.md)); **autorização por perfil** (2026-11-07): tabela de acesso no `SecurityConfig` com o `ADMIN` como padrão e o `CurrentUser` para as regras de dados ([ADR 0026](adr/0026-acesso-por-perfil-centralizado.md)) |
| `provider` | implementado: dois percentuais (padrão 40/45), usuário ativo obrigatório na criação e na reativação ([ADR 0031](adr/0031-prestador-com-usuario-revisto.md)) |
| `pricing` | implementado: grupos, técnicas (com reativação), planos (`PlanType`, `termWeeks`, de-para), preços com reajuste historizado |
| `contract` | implementado: cabeçalho + `ContractItem`, vigência calculada (`SessionFrequency`), aditivos, opções do plano do Grupo, endpoints de apoio, beneficiário de no máximo um contrato vigente e diferente do titular, `current` na resposta (vigente hoje), **valor total e parcelas** (2026-11-12; coluna `installments`, prévia `POST /api/contracts/preview`; [ADR 0032](adr/0032-valor-e-parcelas-do-contrato.md)) |
| `consultation` | implementado: lançamento com contratos vinculados e escolha livre, resolução de preço (casos 1 a 3), `pricingSource`, comissão pela origem, repasse `HALF_UP`, saldo pela cadeia de versões, soft delete sem `PUT`, endpoints de apoio (`launch-options` com as durações válidas, `session-balance`, `patients`, `recent-patients`, `day-summary`); prestador do login para o `PROVIDER`, que só vê e inativa os próprios lançamentos; `launchedByUserId`; **o `PROVIDER` só lança e desfaz com a data de hoje**, e o retroativo usa a liberação do admin (2026-11-13; tabela `retroactive_launch_grant`, no `provider`; [ADR 0033](adr/0033-lancamento-retroativo-com-liberacao.md)); "hoje" no fuso da clínica ([ADR 0034](adr/0034-hoje-no-fuso-da-clinica.md)) |
| **frontend** | **em andamento**: ver a seção [Frontend](#frontend) |

- **Monorepo** desde 2026-10-29 ([ADR 0022](adr/0022-monorepo.md)): o backend em
  `backend/` e o frontend, importado com o histórico, em `frontend/`.
- **Schema versionado pelo Flyway** (V1 a V4, com o seed do catálogo na V4; ver
  [`development.md`](development.md)), com `ddl-auto=validate`. **Até a versão estável, as
  migrations podem ser editadas** ([ADR 0028](adr/0028-migrations-editaveis-ate-versao-estavel.md)):
  a V1 foi editada em 2026-11-07 (colunas `default_admin`, `password_change_required` e
  `launched_by_user_id`) e em 2026-11-12 (coluna `installments` do contrato), então quem puxar
  zera o banco local (`docker compose down -v`). Editada de novo em 2026-11-13 (tabela
  `retroactive_launch_grant`).
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
- **Testes:** até aqui, manuais (pela API e pela interface). Decidido em 2026-11-07 que os
  **testes automatizados** são o próximo passo (ver abaixo).

## Próximos passos

1. **Telas funcionais no front** (decidido em 2026-11-11, antes dos testes): o backend está
   pronto, mas o sistema ainda não funciona de ponta a ponta (contratos e lançamento usam
   mocks e o modelo antigo). **Uma branch e uma spec por tela**, cada uma com o próprio PR,
   na ordem de dependência (as telas de Usuários, Prestadores, Contratos e Lançamento já estão
   prontas; o Lançamento em 2026-11-13, com o retroativo por liberação do admin):
   1. **Aditivo do contrato** (spec própria; decidido em 2026-11-11 deixá-lo fora da tela de
      Contratos e, em 2026-11-13, fazê-lo depois do Lançamento): prorrogar, incluir e remover
      itens, o `group-plan-options`, renegociar as parcelas na prorrogação e como a lista mostra
      as versões (a substituída por aditivo aparece como Inativo). O detalhe do contrato ganha o
      "Editar". Até lá, prorrogar só pelo Swagger.
2. **Testes automatizados no backend** (decidido em 2026-11-07; **pausado** em 2026-11-11
   até as telas funcionarem): cada feature hoje exige dezenas de casos manuais, refeitos a
   cada merge da `dev`, e as regras mais arriscadas são financeiras (resolução de preço,
   comissão, repasse, saldo).
   - **Feito:** o primeiro teste de unidade (`PercentageTest`: o arredondamento `HALF_UP`
     do repasse e a recusa de mais de 2 casas).
   - **Próximo, já levantado:** `ProviderEntity`, com 7 regras (percentual pela origem do
     paciente: trazido por ele, da clínica e trazido por outro; padrão 40/45 no cadastro;
     indicação nunca menor que o padrão; os dois percentuais obrigatórios na alteração;
     reativar exige usuário ativo). Depois, as outras entidades com regra de risco.
   - Depois, **testes de integração** da API (autorização e lançamento primeiro), com o
     MySQL do **Testcontainers** (as migrations usam recursos do MySQL, então o H2 não
     serve). Os 55 casos manuais da autorização são a base.
   - Opcional, durante as telas: ao fechar cada uma, escrever os testes de unidade das
     regras que ela usa.
3. **CI no GitHub Actions**, logo depois dos primeiros testes: a cada PR, compilar e testar
   o backend, build e lint do front e `node scripts/check-docs.mjs`, com o resultado no PR.
   CD (deploy automático) fica para quando houver a versão 1 no ar.
4. **Dashboards**, com spec própria. **Do prestador:** os rendimentos dele (soma dos repasses)
   no dia, na semana e no mês, e os **atendimentos dele com filtros, inclusive por data**: é
   onde ele vê os retroativos, que a tela de Lançamento não mostra (ela é só de hoje). **Do
   admin:** os mesmos dados de todos os prestadores, com o filtro por prestador (decidido em
   2026-11-13). Decidido em 2026-11-06 que o `PROVIDER` vê só os próprios. Em aberto para a
   spec: o que conta como rendimento (quais status, por qual data). O total do dia da tela de
   Lançamento já é do backend (`day-summary`: os ativos de hoje, nos três status). Cobre o item
   "Histórico de atendimentos com filtro por período" do checklist do front.

## Pendências técnicas

- **Antes da versão 1 no ar:** revisar e consolidar as migrations e voltar a regra de
  nunca editá-las ([ADR 0028](adr/0028-migrations-editaveis-ate-versao-estavel.md)); zerar
  o banco de produção.
- **A decidir: comparação de prestadores sem `id`** (encontrado ao planejar os testes,
  2026-11-11). O `isSameAs` da `ProviderEntity` faz `other.getId().equals(this.id)`: com o
  prestador de origem ainda sem `id`, lança `NullPointerException`. Em produção não ocorre
  (todo prestador vem do banco com `id`), mas `Objects.equals(other.getId(), this.id)` não
  quebraria.
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

- `PUT` de grupo, técnica e plano (renomear).
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

## Frontend

Convenções na seção "Frontend" do [`architecture.md`](architecture.md).

### Estado atual

*Atualizado em 2026-11-07; a revisar no inventário.*

Levantado pelos arquivos de `frontend/src/`; **o comportamento de cada tela e se as
chamadas batem com a API real ainda não foram conferidos.**

| Módulo | O que existe |
|---|---|
| `auth` | **concluído e testado pela tela** (2026-11-07): login real, primeiro acesso (a tela troca sozinha para a definição da senha), sessão no `localStorage` que expira pela `expiresAt` e cai num 401, cache limpo ao sair, tela inicial por perfil (`HomeRedirect`), **rotas e menu por papel** (`ProtectedRoute` com `roles`), mensagem da API no `error.message` (interceptor do `apiClient`). Ver "Rotas e sessão" no [`architecture.md`](architecture.md) |
| `patient` | listagem com filtros e busca, modal de detalhes, cadastro em wizard (dados pessoais com a origem do paciente, endereço com ViaCEP, confirmação), ligado à API real (PR #33) |
| `pricing` | catálogo de preços (tabela, busca de técnica, modal de preço) |
| `consultation` | tela de Lançamento (`/consultations`, os dois perfis; 2026-11-13), ligada à API: **barra fixa usada toda pelo teclado** (`Combobox`: paciente, técnica, contrato só com dois, duração com as válidas do backend, status; o Enter pula de campo, e do status leva ao Lançar), **um atendimento por vez**, com aviso de sucesso; pacientes recentes como **sugestão** (técnica e duração em azul); só o aviso de saldo (≤ 2 ou esgotado); data **Hoje** ou **Retroativo** (com liberação do admin); lista de hoje com rolagem própria e "Desfazer" com confirmação; repasse do dia do backend; o `ADMIN` escolhe o prestador. **Testada pela API e pela tela** (2026-11-13) |
| `contract` | tela de Contratos (`/contracts`; 2026-11-13), ligada à API, no modelo real (um item por grupo de preço): listagem com o status Vigente, Vencido ou Inativo (`current` da API), detalhe com o valor total e as parcelas, inativar, e cadastro em **modal grande com rodapé fixo** (plano no `RichSelect` com o alternador Individual \| Familiar, itens em linhas, prévia `POST /api/contracts/preview` com o fim, os valores e as parcelas); erros por campo da API. Primeiro módulo com `display/`, `utils/` e `hooks/` (ver "Dentro de cada módulo" no [`architecture.md`](architecture.md)). **Testada pela API e pela tela** (2026-11-12 e 2026-11-13) |
| `provider` | tela de Prestadores (`/providers`, só `ADMIN`; 2026-11-11): listagem, detalhes (com o status do usuário vinculado) e cadastro/edição em **modal**. O cadastro **vincula um usuário existente** (ativo e sem prestador); percentual vazio usa o padrão do backend; erros por campo da API. No detalhe, a **liberação de lançamento retroativo** (2026-11-13, testada pela tela). Ligada à API; os casos de teste do cadastro ainda não foram rodados |
| `user` | tela de Configurações (`/settings`, os dois perfis): troca do e-mail e da senha com a senha atual; camada `api/` (usuários e `/me`). **Tela de Usuários** (`/users`, só `ADMIN`; 2026-11-10): listagem com busca e filtros, cadastro e alteração com senha provisória, inativar e reativar, admin padrão protegido. Ligada à API e **testada pela tela** (2026-11-11) |
| `app/`, `shared/` | layout com menu lateral, router com rotas `lazy`, `apiClient`, utilitários (`money`, `date`, `masks`, `alert`, `viacep`) e componentes de `shared/components/` |

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
   ordem de cadastro (cada uma depende dos dados da anterior): o aditivo do contrato, depois os
   dashboards; Prestadores, Contratos e Lançamento já estão prontas. O que cada uma fecha e as
   decisões já tomadas: itens 1 e 4 dos [Próximos passos](#próximos-passos) do projeto.
2. **Inventário** das telas que não entram no item 1 (ex: catálogo de preços): o que cada
   uma faz e se as chamadas batem com o contrato real da API (Swagger). Atualizar esta
   seção com o resultado.
3. **Nova identidade visual do portal** (levantado em 2026-11-14): **aguarda o artefato** com a
   identidade, que entra aqui quando o time a validar. Até lá, nada muda no código. Quando
   chegar, uma branch e uma spec própria, com as prévias das telas antes do código. Entram
   juntas a melhoria **"Cores em variáveis CSS em todos os componentes"** (com a paleta toda em
   variáveis, a troca de cores se faz no `index.css`), a pendência da **logo e da `Topbar`** na
   Sidebar e a conferência do **"Visual global em cada tela"**, que passa a seguir a identidade
   nova.

Checklist da Fase 1 no front (herdado do guia original):
- [x] Login funcionando e rotas protegidas.
- [ ] CRUD de `Patient` com endereço e ViaCEP.
- [ ] Catálogo de `pricing` consultável.
- [x] Cadastro de `provider` e `contract`.
- [x] Fluxo completo de lançamento.
- [ ] Histórico de atendimentos com filtro por período.
- [ ] Valores monetários enviados como texto, formatados só na exibição, sem cálculo no front.
- [x] `npm run lint` sem erros e `npm run build` gerando o bundle.

### Pendências

- **7 vulnerabilidades nas dependências** (4 moderadas, 3 altas), acusadas pelo `npm ci`
  em 2026-11-06: avaliar com `npm audit` e atualizar o que for seguro.
- *Em aberto:* **boleto no cadastro de contrato.** O formulário oferece Pix, cartão de crédito,
  cartão de débito e dinheiro; o enum `PaymentMethod` também tem `BANK_SLIP` (um contrato que
  venha com ele aparece como "Boleto"). Decidido deixar assim por enquanto (2026-11-11).
- **Organização dos módulos em `display/`, `utils/` e `hooks/`** (decidida em 2026-11-13, feita
  no `contract`, no `consultation` e no `provider`; ver "Dentro de cada módulo" no
  [`architecture.md`](architecture.md)): levar para cada módulo quando ele for mexido. Arquivos
  ainda na raiz: `roles.js` (auth), `priceAxis.js` (pricing), `filterPatients.js` e
  `filterUsers.js`. Onde um componente tiver muita conta além do layout, ela vai para um hook em
  `hooks/`.
- **Visual global em cada tela** (fundo e bordas frias, cards com sombra, menu e título; feito
  em 2026-11-12 e valendo para todas as telas): conferir, com a tela de Contratos como base,
  Pacientes, Usuários, Prestadores, Preços e Configurações, quando cada uma for mexida (o
  Lançamento já nasceu no visual novo).
- **Sessão sincronizada entre as abas** (`auth`; notado nos testes de 2026-11-13; **decidido**
  manter uma sessão por navegador, com as abas sincronizadas). A sessão fica no
  `localStorage`, que as abas dividem: ao entrar como prestador numa aba, a aba do admin
  continua com o menu do admin (o usuário está na memória do React), mas as requisições levam o
  token do prestador (o `apiClient` lê o `localStorage` a cada uma), e inativar um prestador dá
  "sem permissão". Fazer: o `AuthProvider` ouve o evento `storage` e, quando outra aba entra
  ou sai, atualiza o usuário (ou volta para o login). Mantém o
  [ADR 0024](adr/0024-sessao-frontend-localstorage.md). *Descartado:* uma sessão por aba
  (`sessionStorage`), em que cada aba nova pede login. Para testar dois perfis ao mesmo tempo:
  outro navegador ou uma janela anônima.
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
- Sidebar: trocar a logo em texto pelo arquivo da marca; criar a `Topbar`, se o design
  tiver barra superior.

### Melhorias (sem prioridade)

- **Cores em variáveis CSS em todos os componentes.** Hoje o código está misturado: parte
  dos `.module.css` usa `var(--color-*)` e parte usa o hexadecimal direto (uma regra antiga
  do guia, já removida). Padronizar em variáveis, para a paleta mudar num lugar só. **Em
  parte feito** (2026-11-12): o fundo e as bordas (os dois tons bege) passaram para
  `var(--color-bg)` e `var(--color-border)` em todos os `.module.css`; os outros tons ainda
  estão escritos direto.
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
