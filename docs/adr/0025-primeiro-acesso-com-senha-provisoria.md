# 0025 — Primeiro acesso com senha provisória no próprio hash e troca automática no login

- **Status:** aceito
- **Data:** 2026-11-06

## Contexto

Todo prestador (`Provider`) depende de um usuário (`User`) para acessar o sistema
([ADR 0013](0013-prestador-com-usuario.md)), e não existe cadastro público: quem cria a
conta é o administrador (`POST /api/users`). O projeto não possui servidor de envio de
e-mail (SMTP), então o administrador entrega ao prestador a credencial inicial de acesso.

Ao mesmo tempo, pela LGPD (Art. 46 — medidas técnicas de segurança e rastreabilidade de
acesso), a senha definitiva precisa ser de conhecimento exclusivo do titular da conta: o
administrador não deve saber a senha com que o prestador opera no dia a dia. Além disso,
a restrição técnica definida para a feature é não adicionar novos atributos em
`UserEntity` nem novas colunas/migrations na tabela `user`.

## Decisão

- **Senha provisória identificada por prefixo no próprio campo `password`:** quando o
  administrador cria um usuário (`POST /api/users`) ou informa uma nova senha na edição
  (`PUT /api/users/{id}`), o `UserService` gera o hash BCrypt da senha provisória e o
  grava prefixado com `FIRST_ACCESS:` (`FIRST_ACCESS:$2a$10$...`, 73 caracteres, dentro do
  `VARCHAR(255)` da migration V1). A única exceção é o primeiro administrador criado pelo
  `AdminInitializer` via variáveis de ambiente, que já nasce com senha definitiva (sem o
  prefixo).
- **Detecção automática no `POST /api/auth/login` (sem botão manual na tela de login):**
  - Se o usuário tem o prefixo `FIRST_ACCESS:`, o `AuthService` confere se ele está ativo
    e se a senha digitada bate com o hash provisório (sem o prefixo). Se não bater ou se o
    usuário estiver inativo, responde o mesmo `401 "E-mail ou senha inválidos."` de
    qualquer falha de login, sem revelar que a conta existe ou está em primeiro acesso.
  - Se a senha provisória estiver correta, o login **não emite token JWT** e responde
    `403 Forbidden` (`ProblemDetail` com `detail: "Primeiro acesso pendente. Defina sua
    nova senha para continuar."` e a propriedade `firstAccessRequired: true`).
  - No frontend (`LoginPage`), ao receber `firstAccessRequired: true`, a tela transiciona
    automaticamente para o formulário de definição da nova senha pessoal, reaproveitando o
    e-mail e a senha provisória já digitados.
- **Troca obrigatória no endpoint público `POST /api/auth/first-access`:**
  - Recebe `{ email, provisionalPassword, newPassword }` (mínimo de 8 caracteres em
    `newPassword`).
  - Revalida que o usuário existe, está ativo, está com primeiro acesso pendente e que
    `provisionalPassword` confere com o hash provisório (caso contrário, `401 "E-mail ou
    senha inválidos."`).
  - Exige que `newPassword` seja **diferente** da senha provisória (`400
    BusinessRuleException`: `"A nova senha deve ser diferente da senha provisória."`).
  - Grava o novo hash BCrypt puro (sem `FIRST_ACCESS:`) em `UserEntity.password` e já
    devolve `200 OK` com `LoginResponseDTO` (`token`, `expiresAt`, `user`), concluindo o
    login imediatamente.

## Alternativas descartadas

- **Nova coluna `must_change_password` em `user` e novo atributo booleano em
  `UserEntity`:** exigiria nova migration Flyway para representar um estado que pertence
  exclusivamente à própria credencial armazenada em `password`.
- **Botão "Primeiro Acesso?" na tela de login pedindo apenas o e-mail:** sem exigir a
  senha provisória fornecida pelo administrador, qualquer pessoa que soubesse o e-mail de
  um prestador recém-cadastrado poderia definir a senha antes dele e tomar a conta; além
  disso, o botão manual é desnecessário quando o próprio login detecta a senha provisória
  e redireciona automaticamente.
- **Permitir o login normal com a senha provisória sem forçar a troca:** deixaria o
  prestador operando indefinidamente com uma senha conhecida pelo administrador, ferindo a
  individualidade da credencial e a rastreabilidade exigidas pela LGPD.
- **Convite por link enviado por e-mail (SMTP):** adiciona dependência de serviço externo
  de e-mail e tabela de tokens de ativação sem necessidade para a operação presencial da
  clínica.

## Consequências

- Nenhuma mudança no schema do banco (`V1__initial_schema.sql` permanece intacto) e nenhum
  atributo novo em `UserEntity` (apenas a constante `FIRST_ACCESS_PREFIX` e métodos de
  domínio na entidade).
- Não há como um usuário comum forjar o estado de primeiro acesso digitando
  `FIRST_ACCESS:...` no formulário de senha, pois toda entrada do usuário passa pelo
  `PasswordEncoder.encode(...)` (gerando `$2a$10$...`), e o prefixo só é aplicado
  internamente pelo domínio à saída do hash provisório.
- Caso o prestador esqueça sua senha posteriormente, o administrador pode definir uma nova
  senha provisória em `PUT /api/users/{id}`, o que recoloca o prefixo `FIRST_ACCESS:` e
  obriga o prestador a escolher uma nova senha pessoal no próximo acesso.
