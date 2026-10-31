# Roadmap

Estado do projeto, próximos passos e o que ainda não existe. É o único documento
com status: [`architecture.md`](architecture.md) e
[`domain-model.md`](domain-model.md) descrevem só o que está implementado, e a
feature em andamento fica na spec dela, em [`specs/`](specs/).

## Índice <!-- omit from toc -->

- [Fases](#fases)
- [Estado atual (Fase 1 — atualizado em 2026-10-29)](#estado-atual-fase-1--atualizado-em-2026-10-29)
- [Próximos passos](#próximos-passos)
- [Pendências técnicas](#pendências-técnicas)
- [Melhorias futuras (anotadas, sem prioridade)](#melhorias-futuras-anotadas-sem-prioridade)
- [Não modelado ainda](#não-modelado-ainda)
- [Frontend](#frontend)
  - [Estado atual (a revisar no inventário; atualizado em 2026-10-29)](#estado-atual-a-revisar-no-inventário-atualizado-em-2026-10-29)
  - [Próximos passos](#próximos-passos-1)
  - [Pendências](#pendências)
  - [Melhorias (sem prioridade)](#melhorias-sem-prioridade)

## Fases

- **Fase 1:** cadastros, contratos e lançamento de atendimentos (repasse ao
  prestador e saldo de sessões).
- **Fase 2:** `Payment` e saldo financeiro do paciente (visão da clínica).
- **Fase 3:** app de consulta de saldo para o paciente.

## Estado atual (Fase 1 — atualizado em 2026-10-29)

| Módulo | Situação |
|---|---|
| `patient` | implementado: incl. origem do paciente (`referringProvider`), soft delete e reativação com 409 |
| `user` | implementado: soft delete, e-mail único (409) |
| `provider` | implementado: dois percentuais (padrão 40/45), usuário ativo obrigatório na criação, reativação |
| `pricing` | implementado: grupos, técnicas (com reativação), planos (`PlanType`, `termWeeks`, de-para), preços com reajuste historizado |
| `contract` | implementado: cabeçalho + `ContractItem`, vigência calculada (`SessionFrequency`), aditivos, opções do plano do Grupo, endpoints de apoio, beneficiário de no máximo um contrato vigente |
| `consultation` | implementado: lançamento com contratos vinculados e escolha livre, resolução de preço (casos 1 a 3), `pricingSource`, comissão pela origem, repasse `HALF_UP`, saldo pela cadeia de versões, soft delete sem `PUT`, endpoints de apoio (`eligible-contracts`, `session-balance`) |
| **frontend** | **em andamento**: ver a seção [Frontend](#frontend) |

- **Monorepo** desde 2026-10-29 ([ADR 0022](adr/0022-monorepo.md)): o backend em
  `backend/` e o frontend, importado com o histórico, em `frontend/`.
- **Schema versionado pelo Flyway** (V1 a V3, desde 2026-10-29; ver
  [`development.md`](development.md)), com `ddl-auto=validate`. Banco local criado
  antes disso: zerar uma vez (`docker compose down -v`).
- **Teste manual do fluxo completo pela API** (2026-10-29, comandos manuais, sem script): o
  catálogo, os cadastros, o contrato (incluindo o aditivo), os três casos de
  lançamento, o saldo, a reativação e as constraints do banco funcionaram como
  esperado. Testes automatizados continuam como melhoria futura.
- **Testes:** decidido testar pela **interface gráfica** (não por Swagger nem por
  testes automatizados por enquanto).

## Próximos passos

1. **Autenticação** (backend + tela de login): login com JWT, rotas da API protegidas e
   senha com hash. Spec: [`specs/autenticacao.md`](specs/autenticacao.md).
2. **Interface gráfica** (React + Vite, em `frontend/`) e testes por ela: ver a seção
   [Frontend](#frontend).
3. **Autorização por perfil** (`ADMIN`/`PROVIDER`), depois da autenticação e com spec
   própria:
   - quais endpoints e dados cada perfil acessa: restringir ao `ADMIN` as operações de
     cadastro da clínica, como a listagem completa de técnicas
     (`GET /api/techniques/all`) e as reativações; definir o que o prestador vê;
   - o `launchedByUserId` no `Consultation` (quem lançou) e a regra de permissão do
     lançamento, com o prestador vindo do login;
   - a reativação do `User` e a decisão de exigir usuário ativo para reativar o
     prestador (hoje não exige).

## Pendências técnicas

Backend: nenhuma no momento. Frontend: ver a seção [Frontend](#frontend).

## Melhorias futuras (anotadas, sem prioridade)

- `PUT` de grupo, técnica e plano (renomear).
- Reajuste de preço agendado (`validFrom` futuro).
- Categorias de prestador (Padrão, Estagiário...) com o par de percentuais, se
  surgirem muitas faixas.
- Lista explícita de frequências permitidas por plano, se a clínica quiser
  bloquear alguma combinação abaixo do teto de 52 semanas.
- Código de erro nas respostas (ex: `PATIENT_HAS_CURRENT_CONTRACT`).
- Value object `Cpf` (com dígitos verificadores).
- Carga inicial do catálogo da clínica pronta ao subir. Hoje ele é cadastrado pela
  API; como migration, os `INSERT`s pulariam as regras do domínio (ADR 0021).
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

## Frontend

Convenções na seção "Frontend" do [`architecture.md`](architecture.md).

### Estado atual (a revisar no inventário; atualizado em 2026-10-29)

Levantado pelos arquivos de `frontend/src/`; **o comportamento de cada tela e se as
chamadas batem com a API real ainda não foram conferidos.**

| Módulo | O que existe |
|---|---|
| `auth` | tela de login, sessão no `localStorage`, `ProtectedRoute` (só confere sessão), menu filtrado por papel. **Login mockado** (`authApi.js`): o backend ainda não tem autenticação |
| `patient` | listagem com filtros e busca, modal de detalhes, cadastro em wizard (dados pessoais, endereço com ViaCEP, confirmação). `patientApi.js` com trechos mockados |
| `pricing` | catálogo de preços (tabela, busca de técnica, modal de preço) |
| `consultation` | tela de lançamento (formulário, agenda, carrinho, total). `consultationApi.js` com trechos mockados |
| `provider`, `contract` | só a página placeholder |
| `user` | nada |
| `app/`, `shared/` | layout com menu lateral, router com rotas `lazy`, `httpClient`, utilitários (`money`, `date`, `masks`, `alert`, `viacep`) e componentes de `shared/ui/` |

- **`npm run build` falha** (ver "Pendências").
- Monorepo desde 2026-10-29: o front foi importado com o histórico da `dev` do repositório
  antigo.

### Próximos passos

1. **Corrigir o build** (ver "Pendências"): pré-requisito da autenticação.
2. **Login real**, junto com a autenticação do backend
   ([`specs/autenticacao.md`](specs/autenticacao.md)): trocar o mock do `authApi.js`,
   alinhar os papéis, tela inicial por perfil e mensagens de erro da API.
3. **Inventário:** o que cada tela faz e se as chamadas batem com o contrato real da API
   (Swagger). Atualizar esta seção com o resultado.
4. **Trocar os mocks pelas chamadas reais** onde o backend já tem o endpoint (`patient`,
   `pricing`, `consultation`).
5. **Telas que faltam**, na ordem de cadastro (cada uma depende dos dados da anterior):
   prestadores e usuários → contratos → lançamento ligado à API real. Cada tela começa
   com uma spec em [`specs/`](specs/README.md).

Checklist da Fase 1 no front (herdado do guia original):
- [ ] Login funcionando e rotas protegidas.
- [ ] CRUD de `Patient` com endereço e ViaCEP.
- [ ] Catálogo de `pricing` consultável.
- [ ] Cadastro de `provider` e `contract`.
- [ ] Fluxo completo de lançamento.
- [ ] Histórico de atendimentos com filtro por período.
- [ ] Valores monetários sempre como string, formatados só na exibição.
- [ ] `npm run lint` sem erros e `npm run build` gerando o bundle.

### Pendências

- **Build quebrado** (já estava assim na `dev` do repositório antigo, antes da
  importação): `npm run build` falha com 3 erros de sintaxe, em
  `src/shared/ui/Badge.jsx` (`Expected '}' but found EOF`),
  `src/shared/ui/PageHeader.module.css` (linha 14) e `src/shared/ui/Select.module.css`
  (linha 1), ambos `Unclosed block`. Parecem arquivos cortados num merge.
- **Tela de lançamento planejada em conflito com a API.** O guia original dizia que o
  front envia só `patientId` e `arrivalTime` e que o prestador vem do login. O
  `POST /api/consultations` espera `patientId`, `contractId` (escolhido entre os
  vinculados), `providerId`, `techniqueId`, `durationMinutes`, `occurredAt` e `status`
  (ver `Consultation` no [`domain-model.md`](domain-model.md)). Redesenhar a tela sobre o
  contrato real, com spec.
- **Papéis divergentes:** `domain/auth/roles.js` usa `"admin"` e `"provider"`; o enum
  `Role` do backend é `ADMIN` e `PROVIDER`. Resolvido pela spec de autenticação.
- **Mensagem de erro da API não aparece:** a `ConsultationPage` mostra `error.message`
  (a mensagem técnica do Axios) e o `PriceFormModal` lê `data.message`, campo que o
  `ProblemDetail` não tem (o texto está no `detail`). Corrigir com o
  `getApiErrorMessage` que a spec de autenticação cria em `shared/http/`.
- **Origem do paciente:** conferir se o cadastro de paciente envia o
  `referringProviderId` (prestador que trouxe o paciente), que define a comissão.
- **Menu e rotas por papel:** o admin cai em `/consultations` depois do login, que não
  está no menu dele; `/providers` existe, mas não está no menu do design; Dashboard
  (Fase 2) e Configurações (sem tela definida) ficam no menu, desabilitados. O destino
  de cada papel após o login já foi decidido na spec de autenticação (`ADMIN` →
  `/patients`, `PROVIDER` → `/consultations`, pelo `getHomePath`); falta decidir os
  itens do menu.
- **Fora das convenções** da seção "Frontend" do [`architecture.md`](architecture.md):
  `src/pages/NotFoundPage.jsx` fora da estrutura (mover para `app/`); `PageLoader` e
  `RouteError` com estilo inline; `AuthContext` chamando o HTTP fora da camada `api/`;
  `PatientWizard` com imports relativos (`../../`) em vez de `@/`.
- **`frontend/.env` versionado:** hoje só tem `VITE_API_URL` (sem segredo). Avaliar trocar
  por um `.env.example` versionado e o `.env` no `.gitignore`, antes que entre algum valor
  sensível.
- **Arquivar o repositório antigo do front** (`1k2s/sublime-app-front`) no GitHub, depois
  do merge do monorepo.
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
