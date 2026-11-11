# 0027 — Admin padrão protegido por uma marca no usuário

- **Status:** aceito
- **Data:** 2026-11-07

## Contexto

O primeiro admin é criado pelo `AdminInitializer` a partir de `ADMIN_EMAIL` /
`ADMIN_PASSWORD` ([ADR 0023](0023-autenticacao-jwt-resource-server.md)), sempre que a
aplicação sobe sem nenhum `ADMIN` ativo. Nada impedia inativar ou rebaixar o único admin:
a clínica ficaria sem ninguém para administrar o sistema, e a recuperação seria reiniciar a
aplicação com outro `ADMIN_EMAIL` (o e-mail antigo continua ocupado).

## Decisão

- **O admin criado pelas variáveis é o admin padrão,** marcado por `defaultAdmin` no
  próprio usuário (coluna `default_admin`), gravado só pelo inicializador, nunca pelo
  request.
- **Não pode ser inativado nem mudar de perfil** (409). Os outros usuários, sim. Ele pode
  trocar o próprio e-mail e a senha.
- **Criado só na primeira subida** (banco sem admin padrão), com senha definitiva (sem
  primeiro acesso, [ADR 0025](0025-primeiro-acesso-com-senha-provisoria.md)). Depois disso
  as variáveis são ignoradas: o banco é a fonte da verdade.
- Substitui, do ADR 0023, o critério "ao subir sem nenhum `ADMIN` ativo" para criar o
  primeiro admin.

## Alternativas descartadas

- **Bloquear a inativação ou a troca de perfil do último `ADMIN` ativo:** o admin padrão
  protegido já garante que sempre existe um admin ativo, com uma regra mais simples de
  conferir (uma marca, não uma contagem).
- **Reconhecer o admin padrão pelo `ADMIN_EMAIL`:** quebra quando ele troca o próprio
  e-mail ou quando a variável muda.

## Consequências

- Sempre existe ao menos um admin ativo.
- Trocar `ADMIN_EMAIL` / `ADMIN_PASSWORD` depois da primeira subida não muda nada; o admin
  padrão troca o e-mail e a senha pela aplicação.
- O admin padrão só deixa de existir zerando o banco.
