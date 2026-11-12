# 0031 — Prestador continua sempre com usuário, com o admin lançando por ele

- **Status:** aceito
- **Data:** 2026-11-11

## Contexto

O [ADR 0013](0013-prestador-com-usuario.md) decidiu que todo prestador tem exatamente um
usuário e descartou o usuário opcional porque "o administrador não lança por ele". Deixou a
revisão marcada para quando o admin passasse a lançar por um prestador, o que aconteceu com a
autorização por perfil ([ADR 0026](0026-acesso-por-perfil-centralizado.md)): o `ADMIN` lança
por qualquer prestador. Duas regras dependiam daquele motivo: o cadastro exige um usuário
ativo, e a reativação do prestador também.

## Decisão

- **Mantido:** todo prestador tem um usuário, ativo no cadastro e na reativação (400 "O
  usuário está inativo.").
- **O motivo agora:** o prestador usa o sistema em nome próprio: lança os próprios
  atendimentos e, com o dashboard planejado, vê os próprios rendimentos. O lançamento pelo
  admin é apoio, não substitui o login dele.
- Substitui, do ADR 0013, o motivo da alternativa descartada "usuário opcional" e a
  consequência "esta decisão precisa ser revista".

## Alternativas descartadas

- **Reativar o prestador sem exigir usuário ativo:** serviria ao prestador que volta só para
  atender, com o admin lançando por ele. A clínica não tem esse caso.
- **Usuário opcional:** mudaria o schema, a entidade, o DTO e a tela, para um prestador que
  atende sem usar o sistema, caso que a clínica também não tem.

## Consequências

- Para reativar um prestador com o usuário inativo, o admin reativa antes o usuário (o detalhe
  da tela de Prestadores avisa).
- Se surgir prestador que atenda sem usar o sistema, esta decisão é revista.
