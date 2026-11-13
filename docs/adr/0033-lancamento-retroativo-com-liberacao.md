# 0033 — Lançamento só de hoje para o prestador, com o retroativo liberado pelo admin

- **Status:** aceito
- **Data:** 2026-11-13

## Contexto

O prestador lançava atendimentos com qualquer data não futura. Isso deixava lançar atendimentos
esquecidos semanas depois e fazer correções que mexem num repasse já conferido, sem ninguém
saber. Mas o esquecimento acontece: o prestador precisa de um caminho para lançar o que ficou
para trás, e o admin precisa saber quando isso acontece.

## Decisão

- **O `PROVIDER` só lança e só desfaz com a data de hoje** (`occurredAt` = hoje, o dia da
  clínica: [ADR 0034](0034-hoje-no-fuso-da-clinica.md)). O `ADMIN` lança e desfaz qualquer data
  não futura.
- **Retroativo com liberação:** o prestador pede ao admin, que libera uma **quantidade** de
  lançamentos (1 a 50) para aquele prestador. A liberação vale **até 23:59 do dia em que foi
  dada** ou até a quantidade acabar, e permite **qualquer data passada**.
- Cada lançamento retroativo usa uma unidade; **desfazer não a devolve** (errou, pede de novo).
- **Registro próprio** (`RetroactiveLaunchGrant`, no módulo `provider`): o prestador, o admin que
  liberou, o dia, a quantidade liberada e a usada. Liberar de novo no mesmo dia cria outra, que
  passa a valer; a anterior fica no histórico.
- **Concorrência pela trava no banco** (`PESSIMISTIC_WRITE`): dois retroativos ao mesmo tempo não
  usam a mesma unidade, e o segundo recebe a regra normal (400) se o saldo acabou.
- Na tela, o botão se chama **Retroativo** e mostra as restantes; o admin libera no detalhe do
  prestador.

## Alternativas descartadas

- **Data livre para o prestador** (como era): sem controle nem registro do que foi lançado
  depois.
- **Flag (sim/não) no usuário:** não guarda a quantidade, o prazo nem quem liberou.
- **Liberação por um período de dias:** abre uma janela longa; a quantidade com prazo até o fim
  do dia cobre o caso real (lançar o que foi esquecido) com o menor risco.
- **`@Version` (trava otimista) no lugar da trava no banco:** numa disputa, o segundo lançamento
  daria um erro de concorrência que hoje vira 500; a trava faz o segundo esperar e receber a
  mensagem certa. A disputa é rara e curta (um prestador, poucas vezes por dia).

## Consequências

- Um retroativo não aparece na tela de Lançamento (ela é só de hoje): o prestador o vê no
  dashboard dele, que está no roadmap. Se estiver errado, quem desfaz é o admin.
- A V1 ganhou a tabela `retroactive_launch_grant`
  ([ADR 0028](0028-migrations-editaveis-ate-versao-estavel.md)).
- Com a tela nova, o `GET /api/consultations/eligible-contracts` deu lugar ao
  `GET /api/consultations/launch-options`, que devolve os mesmos contratos vinculados e também as
  durações com preço. O [ADR 0015](0015-contrato-do-atendimento.md) cita o nome antigo; a decisão
  dele (vínculo na data e escolha livre) não muda.
- Revogar uma liberação antes do fim do dia não existe; se fizer falta, entra como um caminho
  novo, que também usa a trava.
