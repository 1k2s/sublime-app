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
| `contract` | implementado: cabeçalho + `ContractItem`, vigência calculada (`SessionFrequency`), aditivos, opções do plano do Grupo, endpoints de apoio, beneficiário de no máximo um contrato vigente e diferente do titular |
| `consultation` | implementado: lançamento com contratos vinculados e escolha livre, resolução de preço (casos 1 a 3), `pricingSource`, comissão pela origem, repasse `HALF_UP`, saldo pela cadeia de versões, soft delete sem `PUT`, endpoints de apoio (`eligible-contracts`, `session-balance`, `patients`); prestador do login para o `PROVIDER`, que só vê e inativa os próprios lançamentos; `launchedByUserId` |
| **frontend** | **em andamento**: ver a seção [Frontend](#frontend) |

- **Monorepo** desde 2026-10-29 ([ADR 0022](adr/0022-monorepo.md)): o backend em
  `backend/` e o frontend, importado com o histórico, em `frontend/`.
- **Schema versionado pelo Flyway** (V1 a V4, com o seed do catálogo na V4; ver
  [`development.md`](development.md)), com `ddl-auto=validate`. **Até a versão estável, as
  migrations podem ser editadas** ([ADR 0028](adr/0028-migrations-editaveis-ate-versao-estavel.md)):
  a V1 foi editada em 2026-11-07 (colunas `default_admin`, `password_change_required` e
  `launched_by_user_id`), então quem puxar zera o banco local (`docker compose down -v`).
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
   na ordem de dependência (as telas de Usuários e de Prestadores já estão prontas):
   1. **Contratos:** o envio no modelo real do contrato (`items`, um por grupo de preço),
      sem mocks, com os erros por campo da API.
   2. **Lançamento:** o redesenho sobre a API real. Decidido: o paciente vem do
      `GET /api/consultations/patients`, o prestador vem do login para o `PROVIDER` e fica
      oculto para ele, o valor aparece só depois de lançar (sem prévia de preço) e o total
      do repasse do dia vem do backend, não da soma no front.
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
4. **Dashboard do prestador**, com spec própria: os
   rendimentos do próprio prestador (soma dos repasses dos atendimentos dele) no dia, na
   semana e no mês. Decidido em 2026-11-06 que o `PROVIDER` vê só os próprios rendimentos.
   Em aberto para a spec: o que conta como rendimento (quais status, por qual data) e se o
   `ADMIN` terá um dashboard da clínica.

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
  Avulso ativo, as mensalidades por prazo), contra o "nenhum outro módulo deve conhecer a
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
| `consultation` | tela de lançamento (formulário, agenda, carrinho, total). `consultationApi.js` com trechos mockados |
| `contract` | tela de contratos (filtros, tabela, formulário com seleção de paciente e preço, resumo), com fallback mockado no `contractApi.js`. **Envio no modelo antigo do contrato** (ver "Pendências") |
| `provider` | tela de Prestadores (`/providers`, só `ADMIN`; 2026-11-11): listagem, detalhes (com o status do usuário vinculado) e cadastro/edição em **modal**. O cadastro **vincula um usuário existente** (ativo e sem prestador); percentual vazio usa o padrão do backend; erros por campo da API. Ligada à API; os casos de teste da tela ainda não foram rodados |
| `user` | tela de Configurações (`/settings`, os dois perfis): troca do e-mail e da senha com a senha atual; camada `api/` (usuários e `/me`). **Tela de Usuários** (`/users`, só `ADMIN`; 2026-11-10): listagem com busca e filtros, cadastro e alteração com senha provisória, inativar e reativar, admin padrão protegido. Ligada à API e **testada pela tela** (2026-11-11) |
| `app/`, `shared/` | layout com menu lateral, router com rotas `lazy`, `apiClient`, utilitários (`money`, `date`, `masks`, `alert`, `viacep`) e componentes de `shared/components/` |

- **Telas de listagem padronizadas** (2026-11-11): Usuários, Pacientes, Prestadores e
  Contratos seguem o mesmo padrão (filtro na page, modal pelo `id`, permissão só no router,
  `ListMessage` com os quatro estados, um hook por endpoint para inativar e reativar). Ver
  "Telas de listagem" no [`architecture.md`](architecture.md). **Teste manual pela tela**
  (2026-11-11), depois da padronização: filtros, inativar e reativar com o modal aberto,
  mensagens da tabela, cadastro e edição nas quatro telas, busca de paciente no contrato e
  no lançamento, e Configurações; tudo funcionou como esperado.
- **`npm run build` funciona** desde a atualização de 2026-11-05 (as correções do PR #13
  do repositório antigo resolveram os arquivos cortados). `npm run lint`: sem erros e sem
  avisos (2026-11-07).
- Monorepo desde 2026-10-29: o front foi importado com o histórico da `dev` do repositório
  antigo e atualizado em 2026-11-05 com os PRs #13 (correções) e #14 (tela de contratos).
  O repositório antigo está arquivado no GitHub.

### Próximos passos

1. **Telas funcionais**, uma branch e uma spec por tela ([`specs/`](specs/README.md)), na
   ordem de cadastro (cada uma depende dos dados da anterior): Contratos (contrato real, sem
   mocks) → Lançamento (API real); Prestadores já está pronta. O que cada uma fecha e as
   decisões já tomadas: item 1 dos [Próximos passos](#próximos-passos) do projeto.
2. **Inventário** das telas que não entram no item 1 (ex: catálogo de preços): o que cada
   uma faz e se as chamadas batem com o contrato real da API (Swagger). Atualizar esta
   seção com o resultado.

Checklist da Fase 1 no front (herdado do guia original):
- [x] Login funcionando e rotas protegidas.
- [ ] CRUD de `Patient` com endereço e ViaCEP.
- [ ] Catálogo de `pricing` consultável.
- [ ] Cadastro de `provider` e `contract`.
- [ ] Fluxo completo de lançamento.
- [ ] Histórico de atendimentos com filtro por período.
- [ ] Valores monetários enviados como texto, formatados só na exibição, sem cálculo no front.
- [x] `npm run lint` sem erros e `npm run build` gerando o bundle.

### Pendências

- **7 vulnerabilidades nas dependências** (4 moderadas, 3 altas), acusadas pelo `npm ci`
  em 2026-11-06: avaliar com `npm audit` e atualizar o que for seguro.
- **Erros por campo da API no formulário de Contratos:** Pacientes, Usuários e Prestadores já
  marcam o `errors` do `ProblemDetail` nos campos (`applyApiFieldErrors`; ver "Camada `api/`"
  no [`architecture.md`](architecture.md)). Falta o formulário de Contratos, que será refeito
  (contrato real): aplicar junto. Até lá, um 400 de validação aparece só no alerta.
- **Regra de negócio no front** (levantado em 2026-11-11; o backend é a fonte da verdade,
  princípio 1 da seção "Frontend" do [`architecture.md`](architecture.md)):
  - **Total de repasse do dia calculado no front:** o `ConsultationTotal.jsx` soma o
    `providerPayoutValue` dos lançamentos do dia (`sumMoney`). Decidir o que entra na soma
    (inativos, status, por qual data) é regra, e é a mesma pergunta em aberto do dashboard
    do prestador. Trocar por um total devolvido pelo backend, junto com o dashboard. Hoje
    soma dados mockados.
  - **Prévia do valor antes de lançar:** o `previewConsultation` (`consultationApi.js`,
    mockado) mostra o valor e o repasse antes do lançamento, o que contradiz a decisão "sem
    prévia de preço: o valor aparece só depois de lançar". Os endpoints previstos nos
    comentários (`/consultations/preview`, `/consultations/batch`) não existem, e o do topo do
    arquivo diz que o `/consultations` não existe (existe). Resolver no redesenho do
    lançamento.
  - **Titular como beneficiário no Zod:** o `contractSchema` recusa o titular como
    beneficiário (`refine`), regra que agora está no backend (`ContractEntity`, 400 "O
    beneficiário deve ser outro paciente."). Na tela nova de Contratos, decidir se o `refine`
    sai, com a busca do beneficiário deixando de oferecer o titular.
- **Tela de lançamento planejada em conflito com a API.** O guia original dizia que o
  front envia só `patientId` e `arrivalTime` e que o prestador vem do login. O
  `POST /api/consultations` espera `patientId`, `contractId` (escolhido entre os
  vinculados), `providerId` (só o `ADMIN` informa; para o `PROVIDER`, vem do login),
  `techniqueId`, `durationMinutes`, `occurredAt` e `status`
  (ver `Consultation` no [`domain-model.md`](domain-model.md)). Redesenhar a tela sobre o
  contrato real, com spec.
- **Tela de contratos no modelo antigo do contrato.** `ContractForm.jsx` e o mock de
  `contractApi.js` usam um preço único no cabeçalho (`groupPlanPriceId` /
  `groupPlanFrequencyPriceId`, campos que não existem na API). O `POST /api/contracts`
  espera `items`: um por grupo de preço, cada um com `techniqueId`,
  `sessionDurationPriceId` **ou** `sessionFrequencyPriceId` e, nos por duração,
  `frequency` (ver `Contract e ContractItem` no [`domain-model.md`](domain-model.md) e o
  [ADR 0008](adr/0008-contrato-com-itens.md)). O formulário também não oferece o boleto
  (`BANK_SLIP`, do enum `PaymentMethod`), e o comentário do `contractApi.js` diz que o
  backend não expõe `/contracts` (expõe). Refazer o envio sobre o contrato real, com spec.
- **Mensagem de erro da API no `PriceFormModal`:** ele lê `error.response.data.message`,
  campo que o `ProblemDetail` não tem, e sempre cai na mensagem própria. Trocar por
  `error.message`, que o interceptor do `apiClient` já preenche com o `detail` da API.
- **Fora das convenções** da seção "Frontend" do [`architecture.md`](architecture.md):
  `PageLoader` e `RouteError` com estilo inline.
- **`frontend/.env` versionado:** hoje só tem `VITE_API_URL` (sem segredo). Avaliar trocar
  por um `.env.example` versionado e o `.env` no `.gitignore`, antes que entre algum valor
  sensível.
- Sidebar: trocar a logo em texto pelo arquivo da marca; criar a `Topbar`, se o design
  tiver barra superior.

### Melhorias (sem prioridade)

- **Cores em variáveis CSS em todos os componentes.** Hoje o código está misturado: parte
  dos `.module.css` usa `var(--color-*)` e parte usa o hexadecimal direto (uma regra antiga
  do guia, já removida). Padronizar em variáveis, para a paleta mudar num lugar só.
- **Regras de dependência entre módulos garantidas pelo ESLint**
  (`import/no-restricted-paths` ou `eslint-plugin-boundaries`): o lint falha quando um
  módulo importa quem não deve.
- **Tradução DTO ↔ tela na camada `api/`:** converter a resposta da API para o formato da
  tela (e de volta), para uma mudança de contrato mexer num arquivo só.
- **Tipos do contrato da API:** decidir sobre TypeScript com tipos gerados do OpenAPI do
  backend (`openapi-typescript`), para o editor acusar quando a API mudar.
- **Testes automatizados** do front.
