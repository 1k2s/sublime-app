# 0001 — Monolito modular com dependência unidirecional

- **Status:** aceito
- **Data:** 2026-10-20 (registro retroativo)

## Contexto

O sistema tem áreas de negócio distintas (cadastro de pacientes, precificação,
contratos, atendimentos), com fronteiras ainda sendo desenhadas e ajustadas. O
produto está no início, com uma clínica e um time pequeno.

## Decisão

- **Um único deploy (monolito), separado em módulos.** Cada módulo é um pacote
  logo abaixo do pacote base, com o nome do agregado raiz que encapsula (pacote
  `contract` → `Contract`).
- **Dependência entre módulos sempre unidirecional.** Um módulo nunca referencia de
  volta um módulo que depende dele (seria ciclo, e quebraria a verificação do
  Spring Modulith, se for adotado).
- **Relacionamentos JPA entre módulos também unidirecionais:** só o lado
  dependente guarda a FK; o lado referenciado nunca tem coleção de volta (ex:
  `Contract → Patient`, e o `Patient` não tem lista de contratos).
- `pricing` reúne técnicas, planos, grupos e preços porque mudam sempre juntos e
  nenhum outro módulo deve conhecer a fiação interna entre eles.

## Alternativas descartadas

- **Microsserviços:** complexidade operacional (vários deploys, comunicação pela
  rede, consistência entre bancos) sem ganho nesta escala. As fronteiras já
  modeladas nos módulos permitem extrair um serviço depois, se for preciso, sem
  reescrever o domínio.
- **Monolito sem módulos:** permitiria acoplamento acidental entre áreas que não
  deveriam se conhecer.

## Consequências

- O mapa de módulos (no [`architecture.md`](../architecture.md)) é a regra: uma
  dependência nova precisa respeitar a direção existente.
- Cálculos que precisariam de uma dependência "para cima" ficam no módulo de cima.
  Ex: o saldo de sessões do contrato é calculado no `consultation`, não no
  `contract`.
- `patient` deixou de ser módulo de base quando ganhou a origem do paciente
  (`patient → provider`, ver [ADR 0012](0012-comissao-pela-origem-do-paciente.md)).
  Continua sem ciclo, porque `provider` nunca referencia `patient`.
