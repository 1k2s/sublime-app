# 0026 — Autorização por perfil: tabela única no `SecurityConfig`, com o `ADMIN` como padrão

- **Status:** aceito
- **Data:** 2026-11-07

## Contexto

Com a autenticação ([ADR 0023](0023-autenticacao-jwt-resource-server.md)), qualquer usuário
logado acessava todos os endpoints. O sistema tem dois perfis: o `ADMIN` controla tudo, e o
`PROVIDER` usa só as telas Dashboard, Lançamentos e Configurações, vendo apenas os próprios
dados. Esconder a tela não basta: qualquer usuário logado chama a API pela URL ou pelo
Swagger, e o papel guardado no navegador pode ser editado.

## Decisão

- **Duas camadas:**
  - **Endpoint:** uma tabela única no `SecurityConfig`, **rota a rota, sem curinga**, com
    os endpoints que o `PROVIDER` usa (`hasAnyRole(ADMIN, PROVIDER)`) e, no fim,
    `anyRequest().hasRole(ADMIN)`. **Endpoint novo nasce restrito ao `ADMIN`.** O `{id}` dos
    lançamentos é só numérico (`{id:\d+}`).
  - **Dados:** o que depende do registro (o `PROVIDER` só lança para ele, só vê e inativa os
    próprios lançamentos) fica no service. O lançamento de outro prestador responde 404,
    com a mesma mensagem de um id inexistente.
- **Quem está logado:** o record `CurrentUser(userId, role)`, no módulo `user`, montado a
  partir do token pelo `CurrentUserArgumentResolver` (no `auth`) e recebido pelos
  controllers como parâmetro. Nenhum módulo de negócio importa o `auth`.
- **403 de regra de dados:** `ForbiddenException` (`shared/exception`), tratada no
  `GlobalExceptionHandler`.
- **Endpoints próprios para o que o `PROVIDER` precisa**, com só o necessário:
  `GET /api/consultations/patients` (id, nome e data de nascimento de pacientes ativos) e
  `GET`/`PUT /api/users/me` (o próprio usuário, sem troca de perfil).

## Alternativas descartadas

- **`@PreAuthorize` em cada método:** as regras ficariam espalhadas pelos controllers, e
  esquecer a anotação deixaria o endpoint aberto.
- **Curinga por módulo** (ex: `/api/consultations/**`): abriria ao `PROVIDER` os endpoints
  futuros do módulo. Pelo mesmo motivo, um `{id}` sem restrição casaria com qualquer rota
  nova de um segmento.
- **Liberar `GET /api/patients` ao `PROVIDER`** para o lançamento: devolveria CPF, contato e
  endereço de todos os pacientes da clínica.
- **Usar o `PUT /api/users/{id}` nas Configurações:** o request tem o perfil, e o próprio
  usuário poderia se promover a `ADMIN`.
- **`AccessDeniedException` do Spring no service:** os módulos de negócio passariam a
  depender do Spring Security.

## Consequências

- Esquecer de configurar um endpoint nega o acesso, nunca o abre. Abrir um endpoint ao
  `PROVIDER` exige acrescentá-lo à tabela.
- Toda regra de acesso aos dados fica no service do módulo, que recebe o `CurrentUser`.
- As telas do front (menu e rotas por papel) só melhoram a experiência: quem protege é o
  403 da API.
