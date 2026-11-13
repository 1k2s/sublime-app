# Sublime Fisioterapia — Arquitetura

## Índice <!-- omit from toc -->

- [Sobre o projeto](#sobre-o-projeto)
- [Convenção de idioma (importante, sempre seguir)](#convenção-de-idioma-importante-sempre-seguir)
- [Arquitetura: monolito modular](#arquitetura-monolito-modular)
  - [Mapa de módulos (Fase 1)](#mapa-de-módulos-fase-1)
- [Princípios de modelagem já fixados (não reabrir sem justificativa forte)](#princípios-de-modelagem-já-fixados-não-reabrir-sem-justificativa-forte)
- [Padrão de construção (todos os módulos implementados seguem)](#padrão-de-construção-todos-os-módulos-implementados-seguem)
- [Formato dos erros](#formato-dos-erros)
- [Segurança](#segurança)
- [Frontend](#frontend)
  - [Princípios](#princípios)
  - [Stack](#stack)
  - [Estrutura de pastas](#estrutura-de-pastas)
  - [Mapa de módulos e regras de dependência](#mapa-de-módulos-e-regras-de-dependência)
  - [Camada `api/`](#camada-api)
  - [Estado](#estado)
  - [Quando criar um componente](#quando-criar-um-componente)
  - [Telas de listagem](#telas-de-listagem)
  - [Componentes: tamanho e layout](#componentes-tamanho-e-layout)
  - [Estilo](#estilo)
  - [Rotas e sessão](#rotas-e-sessão)
  - [Convenções de nomes](#convenções-de-nomes)

## Sobre o projeto

Sistema de controle de atendimento e repasse de honorários para a clínica Sublime
Fisioterapia. Serve dois propósitos: entregar um sistema de negócio funcional e
funcionar como exercício prático de arquitetura de software e inglês técnico.

**Stack:** Java + Spring Boot (monolito modular), MySQL, React + Vite. Versões, setup e
ambiente: [`development.md`](development.md). Fases, estado atual e próximos
passos: [`roadmap.md`](roadmap.md).

Este documento cobre os dois lados: as seções a seguir descrevem o **backend** e as
convenções do projeto inteiro (como a convenção de idioma), e a seção
[Frontend](#frontend), no fim, descreve o front.

## Convenção de idioma (importante, sempre seguir)

Toda nomenclatura de código — classes, atributos, métodos, pacotes — em **inglês**.
Comentários no código e toda documentação/discussão em **português**. Não misturar:
nunca criar uma classe ou atributo com nome em português.

**Mensagens ao usuário em português.** Todo texto que chega na resposta da API é
em português: mensagens das exceptions (`BusinessRuleException`,
`ConflictException`, `ResourceNotFoundException`), mensagens de validação dos DTOs
(`message = "..."`) e os textos do `GlobalExceptionHandler`. Nomes de campo citados
na mensagem aparecem como no JSON (ex: "`frequency` é obrigatória"), porque são o
que o frontend envia.

**Exceção deliberada:** conceitos específicos do contexto brasileiro sem
equivalente correto em inglês mantêm o nome em português — ex: `cep` em
`Address` (não é o mesmo conceito de um "zip code" americano, então traduzir
seria impreciso, não só uma questão de idioma). Isso é exceção pontual, não
abertura geral: qualquer novo caso deve ter a mesma justificativa (conceito de
domínio sem tradução fiel), não conveniência.

## Arquitetura: monolito modular

Um único deploy, separado em módulos com fronteiras de domínio
([ADR 0001](adr/0001-monolito-modular.md)).

- Cada módulo é um pacote logo abaixo do pacote base, nomeado com o mesmo nome do
  agregado raiz que encapsula (ex: pacote `contract` contém a classe `Contract`).
- **Dependência entre módulos é sempre unidirecional.** Um módulo nunca deve
  referenciar de volta um módulo que depende dele — isso cria ciclo e quebra a
  verificação de monolito modular (se formos usar Spring Modulith).
- Relacionamentos JPA entre agregados de módulos diferentes são sempre
  unidirecionais: só o lado "dependente" guarda a FK/referência de objeto; o lado
  referenciado nunca tem coleção de volta.

### Mapa de módulos (Fase 1)

```
pricing          user          ← módulos de base, sem dependência
   |            /    \
   |      provider    auth     ← provider e auth referenciam user
   |         |
   |      patient              ← referencia provider (origem do paciente)
    \       /
    contract                   ← referencia patient + pricing
        \
     consultation              ← depende de todos os anteriores
```

- `pricing`: técnicas, planos e catálogo de preços (`Technique`, `Plan`,
  `PricingGroup`, `SessionDurationPrice`, `SessionFrequencyPrice`). Ficam juntos porque
  mudam sempre em conjunto e nenhum outro módulo deve conhecer a fiação interna
  entre eles.
- `user`: usuários de acesso ao sistema (`User`: e-mail, senha com hash, perfil) e o
  `CurrentUser` (quem está logado), que os módulos de negócio recebem sem conhecer o
  token (ver "Segurança").
- `auth`: login, emissão e validação do token e as regras de acesso da API (ver
  "Segurança"), referencia `user`. O `user` não importa nada do `auth`: recebe só o
  `PasswordEncoder`, que é uma interface do Spring Security.
- `provider`: dado de negócio do prestador (`Provider`), referencia `user`.
- `patient`: cadastro de pacientes (`Patient`), referencia `provider` — a origem
  do paciente (prestador que o trouxe; `null` = clínica) define o percentual de
  repasse ([ADR 0012](adr/0012-comissao-pela-origem-do-paciente.md)).
- `contract`: contrato do paciente (`Contract` + itens `ContractItem`, um por grupo
  de preço contratado), referencia `patient` + `pricing`.
- `consultation`: lançamento de atendimento (`Consultation`), referencia todos os
  anteriores. É o módulo mais "no topo" da cadeia de dependência; por isso o saldo
  de sessões do contrato é calculado aqui, não no `contract`.

Entidades, atributos e regras de negócio: [`domain-model.md`](domain-model.md) —
consulte esse arquivo sempre que for mexer em qualquer entidade de domínio, antes
de propor mudanças de schema.

## Princípios de modelagem já fixados (não reabrir sem justificativa forte)

1. **Value Objects, não entidades**, para dados sem identidade própria. Duas
   estratégias de persistência, escolhidas pela quantidade de campos do VO
   ([ADR 0002](adr/0002-value-object-de-um-valor.md)):
   - **Vários campos → `@Embeddable` + `@Embedded`, como `class`** (ex: `Address`
     dentro de `Patient`). Cada campo vira uma coluna. Precisa ser `class` porque o
     Hibernate instancia o VO sozinho: construtor sem argumentos `protected` e
     campos não-`final`, preenchidos por reflexão.
   - **Um valor só → `record` + `AttributeConverter` com `autoApply = true`** (ex:
     `Money` + `MoneyConverter`, no módulo `pricing`; `Percentage` +
     `PercentageConverter`, no módulo `provider`). O VO fica no módulo dono do
     conceito, sem criar dependência nova entre módulos. Quem cria o VO é o
     converter (via `Money.of`), nunca o Hibernate, então o VO fica livre de JPA,
     imutável e com o construtor validado como única porta de entrada. A coluna
     mantém o nome e a precisão do `@Column` de cada campo, sem
     `@AttributeOverride`.
   - Os DTOs continuam com os tipos do JSON (ex: `BigDecimal`); o service monta o
     VO e o passa à entidade (ex: `PatientService.toAddress`, `Money.of` no
     `PricingService`).
   - Formatação de exibição (ex: `R$ 1.234,56`) não é regra de negócio: fica no
     frontend, nunca no VO. A API devolve número.
2. **Nunca persistir dado derivável que serve só para exibição** (ex: percentual de
   desconto). Sempre persistir dado que já representa uma obrigação financeira
   consumada (ex: valor de um atendimento já lançado).
3. **Historização por `validFrom`/`validTo`, nunca `UPDATE` direto**, em qualquer
   tabela de preço. Reajuste sempre fecha a linha antiga e abre uma nova
   ([ADR 0003](adr/0003-preco-historizado.md)).
4. **"Exclusive arc"**: quando uma entidade pode referenciar uma de duas tabelas
   diferentes (nunca as duas), usar duas FKs nullable garantindo exatamente uma
   preenchida. Usado em `ContractItem` e `Consultation` para
   `sessionDurationPriceId` / `sessionFrequencyPriceId`. Garantido no código
   (`ContractItemDraft` e `@AssertTrue` no DTO; validação no construtor do
   `ConsultationEntity`) e no banco, por um `CHECK` (migration V2; ver "Banco e
   schema" no [`development.md`](development.md)).
5. **Snapshot de valores financeiros no momento em que viram obrigação.** Valores
   tecnicamente deriváveis (valor da sessão, percentual aplicado, repasse) são
   gravados quando representam obrigação de pagamento já consumada — nunca devem
   mudar retroativamente se o catálogo de preço ou a comissão do prestador mudar
   depois. Ex: `baseValue`, `commissionPercentageApplied` e `payoutValue` do
   `Consultation`.
6. **Regra fixa por valor de enum vira método no enum**, não coluna extra no banco
   nem `if` espalhado pelo código (ex: `PlanType.isContractable()`,
   `SessionFrequency.weeksToComplete(...)`).
7. **Entidade rica, não anêmica: cada validação mora na camada certa.** Critério:
   *"para verificar essa regra, preciso consultar outros registros no banco?"*
   - **DTO — formato do request.** Campos obrigatórios, tamanho, e-mail, e
     combinação de campos do request via `@AssertTrue` (ex: exclusive arc e
     frequência em `ContractItemRequestDTO`). Devolve todos os erros de uma vez.
   - **Entidade — regras dela, que não precisam do banco.** Ficam nos
     construtores/fábricas e nos métodos de mudança de estado, para valer em
     qualquer caminho que crie ou altere a entidade (ex: fábricas
     `ContractItemDraft.withDurationPrice/withFrequencyPrice`, endereço completo em
     `Address`). Regra de um valor isolado mora no VO dele (ex: comissão 0–100% no
     `Percentage`, valor não negativo no `Money`). Pergunta sobre si mesma é
     respondida pela própria entidade (ex: `SessionDurationPrice.isCurrent()`).
     Lançam `BusinessRuleException` (400).
   - **Validação em método privado com nome descritivo**, nunca `if` direto no
     corpo do construtor/fábrica: o construtor só chama os validadores e atribui
     os campos (ex: `PatientEntity.validatePersonalInfo`,
     `PlanEntity.validateAmountsForType`). Métodos de mudança de estado seguem o
     mesmo padrão (ex: `requireCurrent()`, `requireAmendable()`).
   - **Service — regras que dependem do banco + orquestração.** Unicidade e
     consultas a outros registros (ex: CPF já cadastrado, paciente com contrato
     vigente), buscar entidades por id, chamar a entidade, salvar.
   - Construtores de negócio (e fábricas) são a **única porta de entrada**: nunca
     `@AllArgsConstructor` em entidade — ele gera um construtor público que ignora
     todas as regras. O construtor sem argumentos fica `protected` (uso do
     Hibernate). Value objects seguem a mesma ideia: o construtor com todos os
     campos é o próprio construtor de negócio e valida o VO (`Address` escrito à
     mão; `Money`/`Percentage` no construtor compacto do `record`).
   - **Sem `@Setter` em entidade** (nem `protected`, que também libera acesso ao
     pacote inteiro). Estado só muda por métodos de negócio (`update`,
     `deactivate`, `updateAddress`...), que aplicam as regras. O Hibernate não
     precisa de setters: as annotations ficam nos campos.
   - Regra de negócio pode aparecer no DTO **e** na entidade quando convém
     (ex: comissão): o DTO dá a mensagem junto com os demais erros de formato; a
     entidade garante a regra para qualquer chamador.
8. **Agregado: a raiz é a única porta de entrada.** Agregado é o conjunto de
   entidades e value objects que mudam juntos e precisam estar sempre coerentes
   como um todo; a **raiz** é a entidade por onde tudo passa. A maioria dos
   agregados do projeto é uma entidade com seus VOs (ex: `Patient` + `Address`).
   Quando o agregado tem **entidade interna** (ex: `Contract` + `ContractItem`,
   [ADR 0008](adr/0008-contrato-com-itens.md)):
   - a entidade interna não tem repositório nem service próprios, e o construtor
     dela não é público — só a raiz cria e altera (no `contract`, via rascunhos
     `ContractItemDraft` montados pelo service);
   - é salva junto com a raiz (`cascade`); a coleção da raiz é exposta como cópia
     somente leitura;
   - regras que olham o conjunto (ex: um item por grupo, fim calculado pelos
     itens) ficam na raiz;
   - outros agregados referenciam só a raiz (ex: `Consultation` → `Contract`, nunca
     → `ContractItem`).
   - A coleção raiz → entidade interna é permitida: a proibição de coleção de volta
     vale **entre módulos**, e os dois são o mesmo agregado e o mesmo módulo.

## Padrão de construção (todos os módulos implementados seguem)

| Camada | Padrão | Precedente |
|---|---|---|
| Controller | construtor explícito (sem `@RequiredArgsConstructor`); `@Tag` + `@Operation` + `@ApiResponses` + `@Parameter` em português; POST → 201, DELETE (soft delete) → 204, 409 se já inativo | `PatientController`, `TechniqueController` |
| DTO | `record`; request com `message` em português em toda anotação de validação; combinação de campos via `@AssertTrue`; response com `fromEntity` estático; **DTO de alteração separado** quando um campo não pode mudar | `PlanRequestDTO`, `ProviderUpdateRequestDTO`, `ContractItemRequestDTO` |
| Service | **um por agregado**; construtor explícito; `@Transactional` (`readOnly` em leitura); `getXOrThrow` (404); `existsBy...` (409); sem `save()` depois de alterar entidade carregada (dirty checking); soft delete = `deactivate`; monta VOs e rascunhos a partir do DTO | `PatientService`, `ProviderService`, `ContractService` |
| Entidade | construtor de negócio (ou fábrica) + `private static validate...` com nome descritivo; `active = true` no campo; `deactivate()` com `requireActive()` (409); sem `@Setter`/`@AllArgsConstructor`; construtor vazio `protected` | `PatientEntity`, `PlanEntity`, `ContractEntity` |
| Enum com regra | regra fixa por valor vira método do enum (princípio 6), com nome de pergunta (`is...`, `has...`) | `PlanType`, `SessionFrequency` |
| VO de um valor | `record` + `AttributeConverter` com `autoApply`; DTO continua `BigDecimal`, service faz `X.of(...)`; rejeita mais de 2 casas, nunca arredonda | `Money` (pricing), `Percentage` (provider) |
| Unicidade | `unique = true` na coluna (garantia real) + `existsBy...` no service (409 com mensagem clara). Collation do MySQL ignora maiúsculas/acentos | `PatientRepository.existsByCpf` |
| Consulta com relação | `@EntityGraph` quando a resposta usa a relação (evita N+1) | `TechniqueRepository.findByActiveTrue`, `PlanRepository.findAll`, `ConsultationRepository.search` |
| Consulta que o nome do método não expressa | `@Query` (JPQL) com `@Param`, só quando o método derivado não dá conta (ex: "ou" com `left join`, filtros opcionais `(:x is null or ...)`); senão, método derivado | `ContractRepository.findActiveCoveringDateByHolderOrBeneficiary`, `ConsultationRepository.search` |
| Endpoint de apoio à tela | leitura que expõe uma regra do backend para o frontend não repetir | `GET /api/techniques/{id}/prices`, `POST /api/contracts/preview` |

## Formato dos erros

`GlobalExceptionHandler`, sempre `ProblemDetail` (RFC 7807), mensagens em
português:

| Origem | Status | Corpo |
|---|---|---|
| Validação do DTO (`@Valid`) | 400 | `detail` genérico + mapa **`errors`** (campo → mensagem; campo aninhado como `address.cep`; `@AssertTrue` aparece com o nome do método sem `is`) |
| JSON ilegível (enum inexistente, tipo errado, data fora do formato, JSON malformado) | 400 | `detail` citando o campo (e, em enum, os valores aceitos; em data, o formato `AAAA-MM-DD`) |
| Parâmetro de URL com tipo errado (ex: `/api/patients/abc`) | 400 | `detail` citando o parâmetro |
| `BusinessRuleException` (regra da entidade/VO) | 400 | `detail` |
| `ResourceNotFoundException` | 404 | `detail` |
| `ConflictException` (duplicidade, estado inválido) | 409 | `detail` |
| `DataIntegrityViolationException` (constraint que escapou das checagens) | 409 | `detail` genérico |
| Login recusado (`AuthenticationException` no `POST /api/auth/login` ou `POST /api/auth/first-access`) | 401 | `detail`: "E-mail ou senha inválidos." (o mesmo para e-mail inexistente, senha errada e usuário inativo) |
| Primeiro acesso pendente (`FirstAccessRequiredException` no `POST /api/auth/login` quando a senha provisória está correta) | 403 | `detail`: "Primeiro acesso pendente. Defina sua nova senha para continuar." + propriedade `firstAccessRequired: true` |
| Rota protegida sem token, com token inválido ou vencido, ou de usuário inativado / com perfil alterado | 401 | `detail`: "Sessão inválida ou expirada. Faça login novamente." + header `WWW-Authenticate: Bearer` |
| Endpoint fora do perfil (`SecurityConfig`, ver "Segurança") | 403 | `detail`: "Você não tem permissão para acessar este recurso." |
| `ForbiddenException` (regra de acesso aos dados, no service: ex. `PROVIDER` lançando para outro prestador ou sem prestador vinculado) | 403 | `detail` |

Os 401/403 de rota protegida acontecem nos filtros de segurança, antes do Spring MVC:
quem os escreve é o `auth` (`ProblemDetailAuthenticationEntryPoint`,
`ProblemDetailAccessDeniedHandler`, pelo `ProblemDetailResponseWriter`), no mesmo formato
`ProblemDetail`. O 403 do `ForbiddenException` não: sai do service, pelo
`GlobalExceptionHandler`.

Ordem em que os erros acontecem: token (401) → perfil (403) → leitura do JSON → DTO
(todos os erros de uma vez) → service (403/404/409, um por vez) → entidade (400, um por
vez) → banco. O
frontend trata `errors` como erro de formulário (por campo) e `detail` como
mensagem geral.

## Segurança

Autenticação por **token JWT emitido pelo próprio sistema** e validado pelo resource
server do Spring ([ADR 0023](adr/0023-autenticacao-jwt-resource-server.md)). Código no
módulo `auth`.

- **Login:** `POST /api/auth/login` (e-mail e senha) → `{ token, expiresAt, user }`. O
  `AuthenticationManager` (`DaoAuthenticationProvider` + `DatabaseUserDetailsService`)
  confere se o usuário existe e está ativo, e a senha contra o hash BCrypt.
- **Primeiro acesso ([ADR 0025](adr/0025-primeiro-acesso-com-senha-provisoria.md)):** se o
  usuário autenticado no login estiver com senha provisória (`passwordChangeRequired`,
  [ADR 0029](adr/0029-senha-provisoria-por-coluna.md)), o `AuthService` não emite o token e
  lança `FirstAccessRequiredException` (`403` com `firstAccessRequired: true`). A troca é
  concluída no endpoint público
  `POST /api/auth/first-access` (`{ email, provisionalPassword, newPassword }`), que exige
  nova senha diferente da provisória, grava o hash BCrypt definitivo e devolve
  `{ token, expiresAt, user }`.
- **Token:** HMAC SHA-256 (`JwtConfig`, chave do `JWT_SECRET`), válido por 8 horas
  (`jwt.expiration`), sem refresh token. Claims: `iss` = `sublime-app`, `sub` = e-mail,
  `userId`, `roles` (lista, ex: `["ADMIN"]`), `iat`, `exp`. Gerado pelo `TokenService`.
- **Em cada requisição** o `JwtDecoder` confere a assinatura, a validade e o emissor e, por
  fim, o `ActiveUserTokenValidator`: o usuário do token precisa existir, estar ativo, sem
  primeiro acesso pendente e com o mesmo perfil do token (uma consulta por chave
  primária). Se não, 401: inativar, mudar o perfil ou redefinir a senha corta a sessão.
- **Perfis:** a claim `roles` vira as autoridades `ROLE_ADMIN` / `ROLE_PROVIDER`, usadas
  pelo `hasRole(...)` / `hasAnyRole(...)` do `SecurityConfig`.
- **Autorização por perfil ([ADR 0026](adr/0026-acesso-por-perfil-centralizado.md)),** em
  duas camadas:
  - **Endpoint (`SecurityConfig`):** uma tabela única, rota a rota e sem curinga (o
    `{id}` só numérico), com os endpoints que o `PROVIDER` usa (`hasAnyRole`). No fim,
    `anyRequest().hasRole("ADMIN")`: **endpoint novo nasce restrito ao ADMIN**, e esquecer
    de configurar nega o acesso, nunca o abre. Quais endpoints o `PROVIDER` acessa:
    "Acesso ao sistema", em `User`, no [`domain-model.md`](domain-model.md).
  - **Dados (service):** o que depende do registro (o `PROVIDER` só vê, lança e inativa os
    próprios lançamentos) fica no service, que lança o `ForbiddenException`
    (`shared/exception`), e não o `AccessDeniedException` do Spring, para os módulos de
    negócio não dependerem do Spring Security. O service recebe o
    **`CurrentUser(userId, role)`**. O
    record mora no `user` e é montado a partir do token pelo `CurrentUserArgumentResolver`
    (`auth`, registrado pelo `CurrentUserWebConfig`): os controllers o recebem como
    parâmetro, e nenhum módulo de negócio importa o `auth`. As regras de cada endpoint
    estão no [`domain-model.md`](domain-model.md) (`Consultation` e `User`).
- **Rotas públicas** (`SecurityConfig`, em `auth/config`): `POST /api/auth/login`, `POST /api/auth/first-access`, o
  Swagger (`/swagger-ui.html`, `/swagger-ui/**`, `/api-docs/**`) e o `/error` interno.
  Todo o resto exige `Authorization: Bearer <token>`.
- **Sem sessão no servidor** (`STATELESS`) e **sem CSRF** (o token vai no header, não em
  cookie). **CORS** liberado só para o front de desenvolvimento (`http://localhost:5173`,
  em `/api/**`).
- **Segredos** (`JWT_SECRET`, `ADMIN_EMAIL`, `ADMIN_PASSWORD`) nunca no git: vêm do
  `backend/.env` local ou das variáveis de ambiente (ver
  [`development.md`](development.md)). Sem `JWT_SECRET` válido (32 bytes ou mais), a
  aplicação não sobe.
- **Swagger:** esquema `bearer` em todas as rotas (botão "Authorize"); o login e o primeiro acesso são marcados
  sem exigência de token (`@SecurityRequirements` vazio).

## Frontend

Como o frontend (`frontend/`) é construído. A convenção de idioma é a mesma do projeto
(seção acima): código em inglês; comentários, documentação e textos de tela em português.
As regras de negócio estão no [`domain-model.md`](domain-model.md): o front as **exibe**,
não as decide. Como rodar: [`development.md`](development.md). Estado e pendências: seção
"Frontend" do [`roadmap.md`](roadmap.md).

### Princípios

1. **O backend é a fonte da verdade.** O front não reimplementa regra de negócio: preço,
   saldo de sessões, comissão, repasse, vigência e de qual contrato sai a sessão são
   resolvidos pela API, e a tela mostra o resultado. Uma regra duplicada no front diverge
   na primeira mudança do backend.
   - Validação no front (Zod) é só de **formato**, para dar resposta rápida no formulário
     (campo obrigatório, máscara, tamanho). A validação que vale é a da API, e as mensagens
     de erro dela (`detail`, `errors`) aparecem para o usuário.
2. **Os mesmos módulos e nomes do backend.** Cada módulo do backend tem uma pasta em
   `src/domain/` com o mesmo nome (`patient`, `contract`, `consultation`...). Os campos usam
   os nomes da API. É o DDD do front: a mesma linguagem e as mesmas fronteiras, sem as
   regras.
3. **Autorização é do backend.** Esconder um item do menu por papel é só visual: qualquer
   usuário logado abre qualquer rota pela URL, e o papel guardado no navegador pode ser
   editado. Cada endpoint confere o papel pelo token.
4. **Dinheiro e percentual não são calculados no front.** O valor digitado vai para a API como
   texto com ponto (ex: `"95.50"`, `"30.5"`, lidos como `BigDecimal` exato) e volta como número
   JSON (`95.5` no JavaScript, sem os zeros à direita); o front só o formata na exibição
   (`formatCurrency` e `formatPercentage`, em `shared/utils/money.js`, com `decimal.js`).
   Cálculo oficial é do backend.

### Stack

| Tema | Escolha |
|---|---|
| Linguagem | JavaScript (sem TypeScript); Zod e JSDoc compensam a falta de tipos |
| Build | Vite (template `react`) |
| Estilo | CSS Modules (um `.module.css` por componente) |
| Dados do servidor | TanStack Query (cache, carregamento, erro, invalidação) |
| Rotas | React Router (data router, rotas `lazy`) |
| Formulários | React Hook Form + Zod (`@hookform/resolvers`) |
| HTTP | Axios, num cliente único (`shared/http/apiClient.js`) |

Lista de dependências: [`development.md`](development.md).

### Estrutura de pastas

```
frontend/src/
├── main.jsx                 # monta AppProviders e RouterProvider
├── index.css                # estilo global e paleta (variáveis CSS)
├── app/                     # casca da aplicação
│   ├── AppProviders.jsx     # QueryClientProvider, AuthProvider (providers do React, não o prestador)
│   ├── router.jsx           # rotas, rotas lazy, ProtectedRoute
│   ├── NotFoundPage.jsx     # página de rota inexistente (*)
│   └── layout/              # AppLayout, AppSidebar, navigationItems.js
├── shared/                  # genérico, sem regra de negócio
│   ├── http/                # apiClient.js (axios: baseURL, token, 401) e apiFieldErrors.js
│   ├── utils/               # money, date, masks, alert (SweetAlert2), viacep
│   └── components/          # Button, Card, Input, Select, RichSelect, FormField, Modal,
│                            # FilterPills, ListMessage, Stepper, Badge, PageHeader, PageLoader...
└── domain/                  # um módulo por módulo do backend
    ├── auth/                # sessão, login, ProtectedRoute, roles.js (transversal)
    ├── user/
    ├── pricing/
    ├── provider/
    ├── patient/
    ├── contract/
    └── consultation/
```

Dentro de cada módulo, só as pastas que ele usa:

| Pasta | O que vai lá |
|---|---|
| `api/` | chamadas ao backend (`xxxApi.js`) e hooks do TanStack Query (`useXxx.js`). Ver "Camada `api/`" |
| `components/` | peças do módulo; recebem dados por props |
| `pages/` | telas inteiras, uma por rota; buscam dados pelos hooks e montam os componentes |
| `schemas/` | schemas Zod dos formulários |
| `hooks/` | estado e comportamento de uma tela ou fluxo, sem JSX (ex: `useConsultationCart`, o lançamento em montagem; `useContractDraft`, o contrato em montagem) |
| `context/` | só no `auth` (sessão) |
| `display/` | funções puras e constantes para **mostrar** um valor da API: rótulos, opções dos selects, textos e formatação (ex: `paymentMethods.js`, `contractStatus.js`, `formatInstallments.js`) |
| `utils/` | funções puras para **decidir ou montar** os dados da tela: filtros, cruzamento de listas, a escolha que vira o `id` da API (ex: `filterContracts.js`, `contractPatients.js`, `resolveItemLine.js`) |

Como escolher a pasta de um arquivo novo:

1. **Chama hooks do React?** Se fala com o backend (chamada e cache), vai em `api/`. Se guarda
   o estado ou o comportamento da tela, vai em `hooks/`. Os dois começam com `use` (regra do
   React para quem chama hooks); a pasta diz qual é qual: em `api/`, o dado do servidor,
   compartilhado pelo cache entre telas; em `hooks/`, o que vive enquanto a tela está aberta.
2. **Não chama:** é função pura, testável sem a tela, e nunca começa com `use`. Se serve para
   mostrar, vai em `display/`; se serve para decidir ou montar, em `utils/`. O critério é o
   uso, não o tipo: um arquivo com uma constante e uma função de rótulo (`paymentMethods.js`)
   vai inteiro em `display/`.
3. **Componente com muita conta além do layout** (ex: opções, avisos e prévia de um formulário
   longo): a conta vai para um hook em `hooks/`, e o componente fica com o formulário e o JSX
   (ex: o `ContractForm` com o `useContractDraft`).

*Por que pela pergunta que o arquivo responde:* com tudo na raiz, um módulo com muitas funções
fica difícil de ler; separar por constante e função gera dúvida (o mesmo arquivo tem as duas),
e separar pelo uso não gera.

| Onde | O quê |
|---|---|
| `domain/<módulo>/components/` | usado num módulo só |
| `shared/components/` | genérico e sem regra de negócio, usado por vários módulos |
| `app/layout/` | menu lateral e layout geral |

### Mapa de módulos e regras de dependência

O mapa é o mesmo do backend (ver "Mapa de módulos" acima): um módulo só importa os que
estão abaixo dele, nunca o contrário.

```
pricing          user
   |               |
   |           provider
   |               |
   |           patient        ← lista prestadores (origem do paciente)
    \             /
     contract                 ← paciente + preços
         \
       consultation           ← todos os anteriores

auth  ← transversal: qualquer módulo importa (sessão e papéis); auth não importa nenhum
```

- `shared/` nunca importa de `domain/` nem de `app/`.
- `domain/` nunca importa de `app/`.
- `app/` pode importar de qualquer módulo (é quem monta as rotas).
- Imports sempre pelo alias `@/` (`@/domain/patient/...`), que aponta para `src/`
  (`vite.config.js` para o build, `jsconfig.json` para o editor). O caminho completo deixa a
  regra de dependência visível na revisão.

### Camada `api/`

```
api (xxxApi.js)  →  hook (useXxx.js)  →  components / pages
```

| Camada | O que faz | O que não sabe |
|---|---|---|
| `xxxApi.js` | uma função por endpoint, chama o `apiClient` e devolve os dados | que existe React |
| `useXxx.js` | transforma a chamada em estado de tela: `useQuery` para leitura (com cache), `useMutation` para ação (com invalidação) | como a tela é desenhada |
| components / pages | mostram os dados e disparam as ações | de onde os dados vêm |

```js
// domain/patient/api/patientApi.js — uma função por endpoint
export async function listPatients() {
  const { data } = await apiClient.get("/patients");
  return data;
}

// domain/patient/api/usePatients.js — leitura com cache, escrita com invalidação
export function usePatients() {
  return useQuery({ queryKey: ["patients"], queryFn: listPatients });
}

export function useCreatePatient() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: createPatient,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["patients"] }),
  });
}
```

Regras:
- Pages e components usam só os hooks, nunca o `apiClient` direto.
- Sem JSX e sem regra de negócio em `api/`.
- `queryKey` padronizada por módulo (`["patients", ...]`, `["consultations", ...]`).
- **`QueryClient` com o padrão da biblioteca** (`staleTime: 0`, em `app/AppProviders.jsx`):
  a tela mostra o que está no cache na hora e busca a versão nova por baixo quando é
  montada, quando a janela volta a ter foco e quando a internet reconecta. Assim, o que
  outro usuário mudou aparece ao abrir a tela ou voltar à aba, sem F5.
- **Toda mutation chama `invalidateQueries`** com a chave do módulo no `onSuccess`. Depois
  de salvar, a lista já está montada e a janela já tem foco, então nenhum dos gatilhos
  acima acontece: sem a invalidação, a tela não atualiza.
- **Listas pequenas vêm inteiras** numa requisição, e o hook devolve a lista inteira (sem
  `select`). A busca e os filtros da tela rodam em memória **na page**, por uma função pura
  do módulo (`filterXxx.js`, ver "Telas de listagem"). *Por que não no `select`:* o `select`
  entrega à page só a lista filtrada, e o item aberto no modal some dela ao mudar de
  estado (ex: inativado com o filtro "Ativos"). Filtro no backend só quando a lista
  precisar de paginação.
- Erro da API: a page mostra `showError(error.message)` (`shared/utils/alert.js`) no
  `onError` do `mutate`. Components não chamam o alerta. O `error.message` já chega com a
  mensagem da API: o interceptor de resposta do `apiClient` troca o texto técnico do Axios
  pelo `detail` do `ProblemDetail` ou, quando não houve resposta ou ela veio sem `detail`,
  por uma mensagem de reserva.
- **Erros por campo da API** (`errors` do `ProblemDetail`, ver "Formato dos erros" acima):
  o formulário entrega à page, junto com os dados, uma função `applyApiErrors`, e a page a
  chama no `onError`. Ela usa o `applyApiFieldErrors` (`shared/http/apiFieldErrors.js`), que
  marca cada erro no seu campo (o `FormField` mostra), traduzindo o nome da API quando
  difere (`address.cep` → `cep`; `@AssertTrue` como `passwordValid` → `password`). Num wizard,
  ela também volta para a primeira etapa com erro.
- Sucesso numa tela que não fecha nem navega depois de salvar (ex: Configurações): a page
  mostra `showSuccess(mensagem)` (`shared/utils/alert.js`).

### Estado

| Tipo de dado | Exemplo | Onde fica |
|---|---|---|
| Vem da API | pacientes, contratos, preços, lançamentos | TanStack Query (hooks em `api/`) |
| Global, que não vem de listagem | sessão do usuário | contexto, com o provider em `app/AppProviders.jsx` |
| De uma tela ou fluxo | itens do lançamento em montagem | `useState` ou hook local (`hooks/`) |

- Dado do servidor nunca vai para contexto: o cache do TanStack Query já compartilha a mesma
  lista entre componentes.
- Provider global fica no `AppProviders`; o `main.jsx` não monta providers. Provider que
  serve a uma tela só é montado em volta dela.

### Quando criar um componente

- **Seção da tela com responsabilidade clara** (ex: `PatientTable`, `PatientFilters`) vira
  componente do módulo, mesmo usada uma vez só: a page busca os dados e monta as seções, e
  cada uma fica no seu arquivo.
- **Trecho pequeno, usado uma vez e sem estado próprio** fica na page ou no componente que o
  usa.
- **Peça que se repete em dois ou mais módulos** vai para `shared/components/`, em vez de ser
  copiada. Ela é genérica e recebe tudo por props (ex: `FilterPills` recebe as opções de
  cada tela). Uma variação visual de algo que já existe vira variante (ex:
  `Button variant="danger"`), não CSS próprio no módulo.

### Telas de listagem

Padrão das telas que listam e editam um cadastro (Usuários, Pacientes, Prestadores,
Contratos), escolhido em 2026-11-11 a partir do que cada uma fazia melhor:

- **O hook busca a lista inteira; a page filtra.** `const { data = [], isLoading, isError }
  = useXxx()` e `filterXxx(data, { search, status })`, função pura em `utils/`.
- **Dado de outro módulo na listagem** (ex: o e-mail e o perfil do usuário na tela de
  Prestadores): a page usa o hook de cada módulo (`useProviders` e `useUsers`) e cruza as duas
  listas numa função pura do módulo (`providerUsers.js`), em vez de juntar na `queryFn`. Cada
  lista fica no cache do módulo dono, então uma edição na tela de Usuários aparece em
  Prestadores; a mesma função dá as opções do formulário (usuários sem prestador). Em
  Contratos, o nome do titular e o do beneficiário vêm da lista de pacientes
  (`contractPatients.js`).
- **O modal guarda só o `id`** do item e o lê da lista inteira
  (`items.find((item) => item.id === selectedId)`): depois de inativar ou reativar, a
  invalidação traz a lista nova e o modal já mostra o estado novo, sem correção à mão.
- **Permissão só no router** (`ProtectedRoute` com `roles`): a page não confere o papel nem
  redireciona.
- **Estados da tabela pelo `ListMessage`** (`shared/components`): carregando, erro da API, o
  filtro escondeu todos (`hasRecords`: a lista inteira não está vazia) ou nada cadastrado.
  Cada tabela passa só os textos com o nome do que lista.
- **Inativar e reativar:** um hook por endpoint (`useDeactivateXxx`, `useActivateXxx`), com
  `mutate(id, { onError })` na page e o `isPending` dos dois passado ao modal
  (`isTogglingStatus`), que desabilita os botões durante a requisição (evita o clique
  duplo).
- **Cadastro e edição:** formulário curto (poucos campos, uma etapa) em **modal** (ex:
  Usuários, Prestadores); formulário longo em etapas independentes em **página inteira com
  wizard** (ex: Pacientes); formulário longo em que uma escolha muda o resultado das outras
  (ex: Contratos: o plano muda o valor dos itens, o fim e as parcelas) em **modal grande**
  (`Modal size="large"`) com o conteúdo rolando e um **rodapé fixo** com o resultado e os
  botões, sempre à vista. O modal grande não fecha pelo clique no fundo
  (`closeOnBackdrop={false}`): um clique sem querer perderia o formulário.
- **Busca sem `useTransition`:** com listas pequenas, filtrar a cada tecla é imediato.

### Componentes: tamanho e layout

- Cada componente define o tamanho e o layout dos próprios filhos (altura, padding,
  `display`, `flex`, espaçamento interno).
- O pai define no máximo o tamanho e a posição dos filhos diretos (largura, altura, coluna do
  grid, `gap`), passando um `className`.
- Nenhum nível define tamanho ou layout de um neto: para mudar uma peça, mexe-se só no
  componente que a renderiza.
- Sem seletor de tag ou descendente para alcançar outro componente (`.form button`): passe
  um `className` para ele.

### Estilo

- CSS Modules: um `.module.css` ao lado de cada componente.
- Paleta e medidas globais em variáveis CSS no `index.css` (`--color-bg`, `--color-surface`,
  `--color-primary`, `--color-primary-soft`, `--color-accent`, `--color-danger`,
  `--color-border`, `--color-text`, `--color-title`, `--color-muted`, `--radius`,
  `--shadow-card`). Os componentes usam as variáveis (`var(--color-primary)`), para a paleta
  mudar num lugar só.
- **Cards se destacam do fundo pela sombra (`--shadow-card`), não pela borda.** A borda fica
  para separações dentro do card (linhas da tabela, itens de uma lista, campos).

### Rotas e sessão

- `app/router.jsx`: `createBrowserRouter` com rotas `lazy`. `/login` é pública; as demais
  ficam dentro do `ProtectedRoute` + `AppLayout`, em dois grupos por papel: só `ADMIN`
  (`/patients`, `/pricing`, `/providers`, `/users`, `/contracts`) e `ADMIN` + `PROVIDER`
  (`/consultations`, `/settings`); `*` é a página de não encontrado.
- **`ProtectedRoute`:** sem sessão, leva ao `/login` (e volta à rota pedida depois do
  login); com `roles`, quem não tem o papel vai para a tela inicial do próprio papel. É só
  experiência de uso: a proteção real é o 403 da API (princípio 3).
- **Menu por papel** (`app/layout/navigationItems.js`, campo `roles`): o `ADMIN` vê
  Pacientes, Contratos, Preços, Lançamentos, Prestadores, Usuários e Configurações; o
  `PROVIDER`, Lançamentos e Configurações. Itens sem tela pronta (Dashboard) ficam
  desabilitados.
- **Sessão no `localStorage`** ([ADR 0024](adr/0024-sessao-frontend-localstorage.md)): o
  `signIn` do `AuthProvider` guarda a resposta do `POST /api/auth/login` em
  `sublime.authToken`, `sublime.authUser` e `sublime.authExpiresAt`.
- `apiClient` envia o token salvo em cada requisição e, num 401, apaga o token e encerra a
  sessão (`setUnauthorizedHandler`, registrado pelo `AuthProvider`).
- **A sessão termina** no logout, ao abrir o app com a sessão vencida (pela
  `sublime.authExpiresAt`) ou incompleta, e num 401 da API (garantia final: token vencido
  durante o uso, usuário inativado ou com o perfil alterado).
- **Ao sair, o cache do TanStack Query é limpo** (`queryClient.clear()` no `signOut`), para
  o próximo usuário do mesmo navegador não ver os dados do anterior.
- **Tela inicial por perfil:** a rota `/` (`HomeRedirect`) leva o `ADMIN` a `/patients` e o
  `PROVIDER` a `/consultations` (`homePath` de cada papel, lido pelo `getHomePath`).
- Os papéis ficam em `domain/auth/roles.js` (`ADMIN` e `PROVIDER`); o valor de cada um
  precisa ser idêntico ao enum `Role` do backend.
- A tela de login tem só e-mail e senha: não há "criar conta" nem botão manual de
  primeiro acesso. Usuários são criados pelo admin (`POST /api/users`), com senha
  provisória; no login com a senha provisória, a tela troca sozinha para o formulário de
  definição da senha pessoal (`POST /api/auth/first-access`,
  [ADR 0025](adr/0025-primeiro-acesso-com-senha-provisoria.md)), que já entra logado.
- **Configurações** (`/settings`, `domain/user`): o próprio usuário troca o e-mail e a
  senha pelo `PUT /api/users/me`, confirmando a senha atual. Depois de salvar, o
  `updateUser` do `AuthProvider` atualiza o usuário guardado, e o menu mostra o e-mail
  novo.
- O menu mostra o e-mail do usuário: a resposta do login não traz nome.

### Convenções de nomes

- Componentes e pages: `PascalCase.jsx`, com o `.module.css` de mesmo nome.
- Hooks: começam com `use`.
- Demais arquivos: `camelCase.js`.
