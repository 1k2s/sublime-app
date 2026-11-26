# Spec — Billing (pagamentos e conta do paciente)

O que o paciente deve e o que pagou: registro dos pagamentos, vencimento das parcelas e a conta
de cada paciente, para a clínica agir antes de o paciente ficar devendo muito ou com muito crédito.

Iniciada em 2026-11-26.

## Índice <!-- omit from toc -->

- [1. Status](#1-status)
- [2. Objetivo e escopo](#2-objetivo-e-escopo)
- [3. Regras](#3-regras)
- [4. Desenho do backend](#4-desenho-do-backend)
- [5. Desenho do frontend](#5-desenho-do-frontend)
- [6. Tarefas](#6-tarefas)
- [7. Casos de teste](#7-casos-de-teste)
- [8. Pendências e pontos abertos](#8-pendências-e-pontos-abertos)
- [9. Documentação ao concluir](#9-documentação-ao-concluir)
- [10. ADRs a registrar](#10-adrs-a-registrar)

## 1. Status

*Atualizado em 2026-11-26.*

- **Branches** (PRs para a `dev`, com merge commit; seção 6):
  - `fix/aulas-do-grupo-pela-vigencia`: a correção da R17, antes do billing;
  - `feature/billing` (esta, criada da `dev` em 7ec4b47): o backend e esta spec;
  - `feature/billing-front`: as telas, da `dev` depois do merge do backend.
- **Feito:** a parte 1 da análise (escopo e regras de negócio, seção 3), aprovada em 2026-11-26.
- **Feito também:** a parte 2 da análise, o desenho do backend (seção 4) e das telas (seção 5),
  com as prévias aprovadas em 2026-11-26.
- **Revisão das regras** (2026-11-26), feita em três partes: 1, o dinheiro do contrato (R17 a
  R23); 2, os pagamentos (R24 a R31); 3, a conta e a lista (R32 a R40).
- **Pré-requisito:** a correção das aulas do Grupo pela vigência do contrato (R17), numa branch
  `fix/` no `contract`, antes do billing.
- **Feito também:** a seção 4 conferida contra as regras R1 a R40 (endpoints, respostas e
  mensagens de cada tela).
- **Feito também:** os casos de teste (seção 7, blocos A a J).
- **Feito também:** as tarefas de implementação (seção 6, etapas 0 a 8). **A análise está
  completa:** a spec é a base da implementação.
- **Próximo:** a etapa 0 (a correção das aulas do Grupo), na branch `fix/`.
- **O que o código já tem e o billing vai ler** (ver o
  [`domain-model.md`](../domain-model.md)):
  - o consumo: cada atendimento ativo grava o `baseValue` (valor da sessão na linha usada), nos
    três status cobrados;
  - o contrato: o total (sessões × valor travado), o número de parcelas e a forma de pagamento,
    sem vencimento e sem nada pago;
  - o saldo de sessões dos itens por duração, calculado no `consultation` pela cadeia de versões.
- **Não existe nada de pagamento:** o sistema não sabe o que o paciente pagou.

## 2. Objetivo e escopo

Hoje a clínica controla à mão o que cada paciente usou e pagou. O billing faz essa conta: o
consumo (o que o paciente usou) comparado com os pagamentos, e as parcelas com vencimento como a
referência do que se espera receber. Módulo novo, `billing`, **acima** do `consultation` no mapa
de módulos: lê o `contract` e o `consultation`, sem juntá-los (precedente: o saldo de sessões, no
`consultation`, lendo o `contract`).

**Entra:**

- registrar e inativar pagamentos (só o `ADMIN`);
- o vencimento das parcelas no contrato (na criação e no aditivo);
- a conta do paciente: o consumido, as mensalidades vencidas do Grupo, o pago e o saldo, por
  contrato e no avulso;
- o saldo de aulas do Pilates em Grupo (controle de presença);
- os avisos: parcela vencida em aberto, consumo acima do pago e consumo acima do contratado;
- as telas.

**Fica para depois:**

- o **encerramento do contrato** e o destino do saldo no fim (abater numa renovação, estornar ou
  congelar; ver "Saldo do paciente" no [`roadmap.md`](../roadmap.md)): no meio do contrato, o
  saldo credor é só pagamento adiantado;
- o relatório financeiro da clínica (o que entrou no mês, quem deve);
- recibo e nota;
- juros e multa por atraso;
- o app do paciente (Fase 3);
- o **pagamento do repasse ao prestador**: outra feature (o dinheiro da clínica para o
  prestador, não do paciente para a clínica).

## 3. Regras

Decididas na parte 1 da análise (2026-11-26).

**R1. A dívida é o consumo** (itens por duração): os atendimentos ativos × `baseValue`, comparados
com os pagamentos. Contam o caso 1 (item do contrato) e o caso 2 (de-para: a técnica fora do
contrato, que grava o contrato de origem). *Por quê:* é o "controle de atendimento" que a clínica
já faz à mão; quem vai mais que o contratado termina devendo, quem desmarca com antecedência
termina com crédito.

Ex: RPG 60 min, 12 × R$ 167,00 = R$ 2.004,00, em 3 parcelas de R$ 668,00 (valores acumulados):

| Mês | Sessões no mês | Consumido | Pago | Conta |
|---|---|---|---|---|
| 1º | 4 | R$ 668,00 | R$ 668,00 | em dia |
| 2º | 3 (desmarcou 1) | R$ 1.169,00 | R$ 1.336,00 | saldo credor de R$ 167,00 (adiantado) |
| 3º | 3 (desmarcou mais 1) | R$ 1.670,00 | R$ 2.004,00 | crédito de R$ 334,00 no fim |

O mesmo contrato, com o paciente indo mais (aviso no 2º mês, não só no fim):

| Mês | Sessões no mês | Consumido | Pago | Conta |
|---|---|---|---|---|
| 1º | 4 | R$ 668,00 | R$ 668,00 | em dia |
| 2º | 6 | R$ 1.670,00 | R$ 1.336,00 | deve R$ 334,00 (aviso) |
| 3º | 4 | R$ 2.338,00 | R$ 2.004,00 | deve R$ 334,00: 14 sessões, 2 além das 12 |

**R2. Pilates em Grupo: duas contas separadas, que não se afetam.**
- **Dinheiro:** a dívida são as **mensalidades vencidas** (R18): o valor do item ÷ os meses do
  contrato (Trimestral 2x: 26 aulas × R$ 65,81 = R$ 1.711,06 ÷ 3 meses). As aulas lançadas **não**
  entram no consumido: faltar ou ir mais não muda a mensalidade.
- **Aulas (controle):** as contratadas (as **semanas do contrato** × vezes por semana, R17, a mesma
  conta do valor do item) − as lançadas. Mostra se o paciente está desmarcando muito ou indo além.
  Muda a regra atual "o item do Pilates em Grupo não tem saldo de sessões".
- **Sem o aviso de saldo na tela de Lançamento** (o de ≤ 2 ou esgotado, dos itens por duração):
  no Grupo ele daria a entender que a aula será cobrada à parte. O saldo de aulas aparece na conta
  do paciente e no aditivo.
- No contrato misto (ex: Evolução + Grupo), a dívida é o consumo dos itens por duração + as
  mensalidades vencidas do Grupo.
- *Descartado:* a quantidade de aulas pelas sessões do pacote equivalente (de-para): o contrato e
  o billing usam a mesma conta (semanas × frequência), e o valor do contrato não muda.

**R3. Pagamento de valor livre**, ligado ao contrato ou, no avulso, sem contrato. As parcelas são
só a **referência** do que se espera receber. *Descartado:* pagar a parcela ("parcela 2 de 3
paga"): as parcelas não são a dívida, e ligar o pagamento a elas esconderia o consumo.

**R4. Parcelas com vencimento:** a clínica combina com o paciente a melhor data de pagamento, e
ela vai no contrato; uma parcela por mês a partir dela.

**R5. Vencimento e parcelas no aditivo:**
- **O que já foi pago entra na conta:** as parcelas da nova versão dividem **o que falta pagar**
  (o total da nova versão − o pago), não o total inteiro; senão o paciente pagaria de novo o que
  já pagou. Substitui em parte o [ADR 0037](../adr/0037-parcelas-escolhidas-no-aditivo.md), que
  dividia o total porque o sistema ainda não sabia o que tinha sido pago.
- **Vencimento alterado:** se a parcela do mês ainda não foi paga, o novo vencimento vale para o
  mês atual e os seguintes; se já foi paga, a partir do próximo mês.

**R6. Avulso** (atendimento sem contrato, caso 3): entra como **débito** no lançamento; o
pagamento vem depois (na hora ou não), de valor livre e sem contrato. A tela sugere o valor em
aberto, e o admin pode mudar.

**R7. Só o `ADMIN` registra e inativa pagamentos.** O prestador não recebe pelo sistema: só vê a
relação dos atendimentos dele.

**R8. Pagamento imutável:** para corrigir, inativar e registrar de novo, como o lançamento de
atendimento ([ADR 0020](../adr/0020-lancamento-imutavel.md)). Dinheiro com histórico.

**R9. A conta é do titular:** o consumo do beneficiário sai do contrato do titular, como a sessão.

Decididas na parte 2 da análise (2026-11-26):

**R10. Pagamento em contrato inativo é aceito:** o paciente pode quitar uma dívida de um contrato
que já terminou (vencido ou inativado). O contrato precisa ser do paciente, como titular.

**R11. Vencimento nos dias 29 a 31:** no mês mais curto, a parcela vence no último dia do mês
(como os bancos). Cada vencimento sai do 1º (1º vencimento + N meses), não do anterior, para o dia
não "escorregar" (31/01 → 28/02 → 31/03, e não 28/03).

**R12. Lista de quem deve na primeira versão:** uma linha por paciente com o saldo e os avisos,
para a clínica não abrir paciente por paciente.

**R13. O valor parcelado é gravado** (`installmentBase`, por versão): o total, no cadastro; o total
− o pago, no aditivo (R5). Depois não muda. *Por quê:* o pago pode mudar depois (um pagamento com
data retroativa, um pagamento inativado); recalculado na leitura, o valor das parcelas assinadas
mudaria sozinho. O pagamento retroativo muda só o saldo da conta. Substitui em parte o
[ADR 0032](../adr/0032-valor-e-parcelas-do-contrato.md) ("só o número de parcelas gravado").

**R14. Valor bruto e líquido** (2026-11-26): o pagamento grava o **bruto** (o que o paciente pagou)
e o **líquido** (o que a clínica recebe, depois da taxa da maquininha). **A conta do paciente usa o
bruto** (bate com as parcelas: a taxa é custo da clínica, não dívida do paciente); o líquido serve
ao faturamento. O admin digita o líquido, que vem preenchido com o bruto (Pix e dinheiro ficam
iguais; no cartão, corrige pelo valor da maquininha), e a tela mostra a taxa ("Taxa: R$ 26,72 ·
4,0%"). Líquido entre 0 e o bruto. *Descartada:* a taxa cadastrada por forma de pagamento (a real
muda com a bandeira, as parcelas e a maquininha; pode entrar depois só como sugestão).

**R15. Cartão parcelado: um pagamento, com o número de parcelas do cartão** (`cardInstallments`, 1
a 12, só no crédito). Uma passada na maquininha é uma transação: um registro, inativado de uma vez.
Para o paciente, o bruto inteiro conta na data do pagamento (as parcelas dele ficam pagas). Para a
clínica, a **previsão de recebimento** é calculada, não gravada: o líquido ÷ as parcelas do cartão,
uma por mês a partir do mês seguinte (os centavos na primeira). Na tela, o "o que está sendo pago"
sugere o valor ("Parcela em aberto" ou "Todas as parcelas em aberto"). *Descartada:* criar N
pagamentos (corrigir exigiria inativar todos, e o histórico mostraria N pagamentos de uma
transação). Fora da primeira versão: a antecipação de recebíveis.

**R16. Dashboard do admin com duas visões** (o billing oferece os endpoints; a tela é da feature de
dashboards): **Produção** (o consumo, pela data do atendimento: o que a clínica atendeu, recebido ou
não) e **Faturamento** (os pagamentos, pela data do pagamento: bruto, taxas e líquido, por forma de
pagamento). Opcional: **a receber do cartão** (a previsão da R15, mês a mês).

Ex (R13): contrato de R$ 2.004,00; aditivo inclui R$ 1.002,00 com R$ 668,00 pagos:

| Versão | Total | `installmentBase` | Parcelas |
|---|---|---|---|
| Original | R$ 2.004,00 | R$ 2.004,00 | 3 × R$ 668,00 |
| Aditivo 1 | R$ 3.006,00 | R$ 2.338,00 (3.006 − 668) | 1ª R$ 779,34 + 2 × R$ 779,33 |

Decididas na revisão das regras, parte 1, o dinheiro do contrato (2026-11-26):

**R17. As aulas do Grupo seguem a vigência do contrato** (correção no `contract`, pré-requisito do
billing): o item do Grupo vale **as semanas do contrato × vezes por semana × o valor da aula** da
linha travada, e não as semanas do plano do Grupo. *Por quê:* o valor da aula é o que manda; o plano
só dá a condição (frequência e preço da aula). No contrato misto, o Grupo acompanha a vigência e
termina junto, e hoje cobra aulas que não cabem nela.
- Ex: Evolução 2x/semana (6 semanas) + Grupo Trimestral 2x: hoje 13 × 2 = 26 aulas (R$ 1.711,06),
  em 6 semanas que só comportam 12; com a regra, 6 × 2 = 12 aulas × R$ 65,81 = R$ 789,72.
- Contrato só de mensalidade: não muda (a vigência já é o prazo do plano). Mensalidade com item por
  duração (R6 do aditivo, ex: Trimestral + RPG quinzenal, 24 semanas): 24 × a frequência, ao preço
  do Trimestral. Prorrogação no aditivo: as aulas crescem com a nova vigência, o preço da aula é o
  travado.
- Substitui em parte o [ADR 0032](../adr/0032-valor-e-parcelas-do-contrato.md) (o item do Grupo
  pelas "semanas do plano"). *Descartado:* manter as semanas do plano (o contrato misto teria de
  durar ao menos o prazo do plano do Grupo).

**R18. A mensalidade do Grupo:** o valor do item ÷ os meses do contrato (`Installments.maxFor`), com
os centavos na primeira, como as parcelas. Cada mensalidade vence numa data de vencimento do
contrato (o 1º vencimento + N meses, R11), **mesmo com menos parcelas** que meses: quem pagou à
vista fica com saldo credor, abatido mês a mês.

**R19. A inativação do contrato tem data** (`deactivatedOn`): depois dela, nada vence: as parcelas
não ficam atrasadas e o Grupo para de somar mensalidades. A conta do contrato passa a valer só pelo
saldo (consumido × pago); o destino dele é o encerramento (fora desta feature). Na versão substituída
por aditivo, a data é o `createdAt` da sucessora.
- Ex: contrato de 12 meses inativado no mês 4 (o paciente desistiu): sem a data, as parcelas de 5 a
  12 iriam vencendo e marcando "Atrasado".

**R20. O máximo de parcelas no aditivo são os meses que faltam:** do novo 1º vencimento até o fim do
contrato, no mínimo 1 (pela mesma conta de meses do `Installments.maxFor`). Substitui de novo em
parte o [ADR 0037](../adr/0037-parcelas-escolhidas-no-aditivo.md) ("até os meses da nova vigência").
- Ex: contrato de 12 meses com aditivo no mês 10: hoje, até 12 parcelas, vencendo até o mês 22;
  com a regra, até 2.

**R21. O 1º vencimento:**
- **no cadastro:** do início do contrato até 1 mês depois (a 1ª parcela cai no primeiro mês);
- **no aditivo:** a partir da data do aditivo (nenhuma parcela nasce vencida). A prévia sugere
  (R5): se a parcela deste mês está paga, o mesmo dia no mês seguinte; se não, o dia de vencimento
  deste mês, ou o do mês seguinte quando ele já passou.

**R22. O pago que cobre o total no aditivo:** o `installmentBase` tem mínimo 0. Com 0, a prévia
avisa "O pago cobre o total: nada a parcelar", e a versão grava 1 parcela de R$ 0,00 (já paga); o
que passar do total fica como saldo credor na conta.

**R23. O pago das parcelas da versão atual:** o pago da cadeia de versões − o pago antes do aditivo
(total da versão − `installmentBase`), **limitado entre 0 e o `installmentBase`**: um pagamento
retroativo ou inativado nunca deixa a situação das parcelas negativa nem acima de "paga".

Decididas na revisão das regras, parte 2, os pagamentos (2026-11-26):

**R24. A forma de pagamento que conta é a do pagamento.** A do contrato (`Contract.paymentMethod`)
é só uma referência do combinado: a clínica é flexível, e só cobra a forma do contrato de pacientes
difíceis. Nenhuma conta do billing usa a forma do contrato; no "Registrar pagamento" ela só vem
pré-selecionada, e o admin troca. O contrato não muda.

**R25. As formas do pagamento:** Pix, cartão de crédito, cartão de débito e dinheiro (as mesmas do
cadastro de contrato). O boleto (`BANK_SLIP`, que o enum já tem) entra com o gateway.

**R26. A previsão de recebimento por forma** (o faturamento e o "a receber" do dashboard, R16):
Pix, dinheiro e débito contam como recebidos na data do pagamento (o débito cai em D+1: um dia não
muda o mês); o crédito em N vezes, N partes do líquido, na data do pagamento + 1, + 2… meses (o dia
pela R11), com os centavos na primeira.

**R27. As sugestões do "o que está sendo pago"** vêm do backend, só as que valem para o caso:
- **Parcela em aberto:** o que falta da parcela vencida mais antiga (ou da próxima, se nenhuma
  venceu);
- **Todas as parcelas em aberto:** o que falta do `installmentBase` da versão;
- **Saldo devedor:** devido − pago, quando maior que 0 e diferente das outras. Ex: parcelas quitadas,
  mas 2 sessões além das contratadas (R$ 334,00), que as parcelas não cobrem;
- **Outro valor** (o admin digita).
- No avulso: **Débito em aberto** e **Outro valor**.

**R28. Os limites do pagamento:** bruto maior que 0, sem teto (o que passar do devido vira saldo
credor; a tela avisa quando o valor passa do maior em aberto); líquido de 0 ao bruto, em qualquer
forma (o Pix de pessoa jurídica pode ter tarifa); data até hoje; "parcelado em" de 1 a 12, só no
crédito.

**R29. Inativar é corrigir, não estornar.** A inativação (R8) é para um registro errado: o pagamento
não aconteceu daquele jeito. O **estorno** (a clínica devolve dinheiro que recebeu) é outro
movimento: feito por inativação, o histórico diria que o pagamento nunca existiu, e o faturamento
daquele mês mudaria. O estorno fica com o encerramento (fora desta feature).

**R30. Inativar um pagamento de antes do aditivo faz a dívida reaparecer na versão atual**
(comportamento esperado, não erro): o `installmentBase` da versão não muda (R13), e o pago dela
diminui (R23). Ex: o aditivo dividiu R$ 2.338,00 (3.006 − 668 pagos); o pagamento de R$ 668,00 é
inativado (não entrou): as parcelas do aditivo continuam somando R$ 2.338,00, e os R$ 668,00 voltam
a faltar, como saldo devedor da conta.

**R31. O crédito do avulso não passa para o contrato** (limite da primeira versão): avulso e
contrato são contas separadas; mover saldo entre elas é o destino do saldo, do encerramento.

Decididas na revisão das regras, parte 3, a conta e a lista (2026-11-26):

**R32. Os contratos na conta do paciente:** os ativos (vigentes ou vencidos) em que ele é titular,
sempre; os inativados, **só enquanto o saldo for diferente de 0** (a dívida não some); o vigente
primeiro. Cada contrato aparece pela versão atual, com as contas somadas pela cadeia de versões.

**R33. O beneficiário:** a conta é do titular (R9). Na aba Sessões de um contrato com beneficiário,
uma coluna "Paciente" diz quem foi atendido. A conta do beneficiário mostra só o que é dele (o
avulso e os contratos em que ele é titular).

**R34. Quem entra na lista do Financeiro:** o paciente com algum contrato na conta (R32) ou com
avulso lançado ou pago, **inclusive o paciente inativado com saldo** diferente de 0.

**R35. Saldo e situação de quem tem mais de uma conta** (contrato e avulso, ou um contrato
inativado com saldo e um vigente; são contas separadas, R31):
- **na lista**, o saldo é a soma das contas (visão geral); a conta do paciente mostra cada uma;
- **a situação** é avaliada conta a conta, nesta prioridade: **Atrasado** (alguma parcela vencida
  em aberto, em qualquer contrato) → **Devendo** (alguma conta com saldo devedor) → **Com crédito**
  (nenhuma devendo e alguma com saldo credor) → **Em dia** (todas zeradas);
- **Atrasado com saldo credor** é possível e fica assim: ex: pagou R$ 300,00, consumiu R$ 0,00 e a
  parcela de R$ 668,00 venceu (adiantou parte, mas não a parcela combinada): "Atrasado", saldo
  credor de R$ 300,00.

**R36. Avisos, próximo vencimento e ordem da lista:**
- **avisos:** as parcelas vencidas (com a quantidade), as sessões além do contratado (com a
  técnica), o avulso em aberto e o contrato inativado com saldo devedor;
- **próximo vencimento:** a parcela vencida mais antiga, entre todos os contratos; sem vencida, o
  próximo vencimento do contrato vigente;
- **ordem:** os atrasados, da parcela vencida mais antiga para a mais recente; depois os devendo, do
  maior saldo devedor para o menor; depois os em dia e os com crédito, por nome;
- **filtros** (pelos campos da resposta, sem regra no front): **Devendo** = saldo devedor (inclui
  os atrasados que devem); **Atrasados** = alguma parcela vencida em aberto; **Com crédito** = saldo
  credor. Um paciente pode estar em dois filtros (ex: o atrasado com saldo credor da R35).

**R37. A numeração das sessões** ("7ª de 6", calculada pelo backend):
- só os atendimentos **ativos** (o inativado não é cobrado e fica fora da aba);
- por item (o grupo de preço), pela cadeia de versões, pela data e, no mesmo dia, pela ordem do
  lançamento; o contratado é o do item na **versão atual**;
- acima do contratado: **"além do contratado"** (cobrada, R1); no Grupo, **"além das aulas
  previstas"**, sem valor (não cobrada à parte, R2);
- o de-para (caso 2): sem número, com "fora do contrato"; o item removido por aditivo: as sessões
  dele continuam, com "item removido no aditivo";
- inativar um atendimento antigo renumera os seguintes (esperado: a numeração conta o que foi
  cobrado).

**R38. A Produção do dashboard segue a conta** (R16): o consumo dos itens por duração, o de-para e o
avulso, pela data do atendimento, mais as mensalidades do Grupo, pelo vencimento; as aulas do Grupo
não somam (R2). O dashboard e a conta nunca dão números diferentes para o mesmo período.

**R39. O contrato não mostra a situação das parcelas:** o `contract` não lê os pagamentos (o
`billing` fica acima). O detalhe do contrato mostra as parcelas com o vencimento e o valor; a
situação fica no Financeiro (o link "Ver conta").

**R40. A lista sem N+1:** a lista calcula com somas agrupadas no banco (o consumido e o pago por
cadeia de versões), não paciente por paciente (o N+1 das listagens de contratos já está nas
pendências do roadmap).

## 4. Desenho do backend

Aprovado em 2026-11-26 (parte 2a da análise). O recorte das classes pode mudar na implementação.

**Dependência entre módulos: o aditivo precisa do pago, que é do `billing`** (acima do `contract`).
O `contract` declara uma interface, `PaidAmountProvider` (`paidOn(contractChainIds)`), e o
`billing` a implementa; o Spring injeta. O aditivo continua no `contract`, e a dependência continua
apontando para cima. Primeiro caso de inversão de dependência no projeto (vira ADR).
*Descartadas:* o `billing` fazer o aditivo (o fluxo sairia do dono do contrato); o front mandar o
pago (o backend é a fonte da verdade).

**Módulo `billing` (novo)**

| Arquivo | Mudança |
|---|---|
| `domain/PaymentEntity` | `patient`, `contract` (nullable: avulso), `amount` (bruto, `Money`, > 0; R14), `netAmount` (líquido, `Money`, de 0 ao bruto; R14), `cardInstallments` (1 a 12, maior que 1 só no crédito; R15), `paidAt` (não futura), `paymentMethod` (o `PaymentMethod` do contrato), `registeredByUser`, `active`. Fábrica com `validateX`/`requireX`: valor positivo, líquido de 0 ao bruto, "parcelado em" de 1 a 12 e maior que 1 só no crédito, data não futura, forma entre as quatro (R25), contrato do paciente como titular (R10: inativo aceito; R28). `deactivate()`: 409 "O pagamento já está inativo.". Sem alteração (R8) |
| `repository/PaymentRepository` | soma dos ativos pela cadeia de versões (`contract.id in :chainIds`); soma dos ativos sem contrato do paciente; listagem com filtros |
| `service/PaymentService` | registrar, listar, inativar |
| `service/BillingAccountService` | a conta do paciente (os contratos pela R32 e o avulso; as parcelas com a situação; as sessões numeradas, R37; as sugestões do "o que está sendo pago", R27) e a lista de contas (R34 a R36, com somas agrupadas, R40) |
| `service/BillingPaidAmountProvider` | implementa o `PaidAmountProvider` do `contract` |
| `dto/` | um por resposta da tabela "Endpoints e respostas" (abaixo) |
| `controller/PaymentController` | `POST /api/payments`, `POST /api/payments/preview`, `GET /api/payments?patientId&contractId`, `DELETE /api/payments/{id}` |
| `controller/BillingController` | `GET /api/billing/accounts`, `GET /api/billing/patients/{id}/account`, `GET /api/billing/contracts/{id}/installments`, `GET /api/billing/contracts/{id}/sessions`; para o dashboard (R16), `GET /api/billing/production?from&to` e `GET /api/billing/revenue?from&to` |

Tudo só `ADMIN` (R7): o `SecurityConfig` já tem o `ADMIN` como padrão, sem mudança.

**Endpoints e respostas, por tela** (conferido contra R1 a R40 em 2026-11-26). Cada aba da conta
carrega o próprio endpoint ao abrir (precedente: o histórico do contrato, `GET
/api/contracts/{id}/versions`). Toda conta de dinheiro vem pronta do backend: o front só mostra.

| Tela | Endpoint | Resposta (campos principais) |
|---|---|---|
| Lista do Financeiro | `GET /api/billing/accounts` | uma linha por paciente (R34), **já na ordem da R36**: `patientId`, `patientName`, `patientActive`; `contracts` (os rótulos, "Evolução · Aditivo 1"); `due`, `paid`, `balance` (a soma das contas, R35); `status` (`LATE`, `OWING`, `CREDIT`, `UP_TO_DATE`: a prioridade da R35); `hasOverdue`, `overdueCount`, `overdueAmount`, `oldestOverdueDate`; `nextDue` (data e valor, R36); `warnings` (tipo e texto) |
| Conta · Resumo | `GET /api/billing/patients/{id}/account` | o paciente; `contracts` (R32), cada um com `contractId` (a versão atual), `planName`, `amendmentNumber`, `endDate`, `active`; `totalValue`, `paid`, `consumed`, `groupFeesDue`, `due`, `balance`, `status`; `sessionsUsed`; `installments` (o resumo da linha de atalho: pagas, total, atrasadas, a próxima); `items` (por item: rótulo, se é do Grupo, `addedInCurrentVersion` para a etiqueta "Novo no aditivo", usado, contratado, valor usado, valor contratado, excesso em quantidade e valor, zero no Grupo, R2); `warnings`; `paymentSuggestions` (as opções da R27: tipo e valor). E `singleSession` (o avulso: devido, pago, saldo, `paymentSuggestions`) |
| Conta · Parcelas | `GET /api/billing/contracts/{id}/installments` | uma entrada por versão, da mais nova para a mais antiga: `contractId`, `amendmentNumber`, `createdAt`, `current`, `totalValue`, `installmentBase`, `paidBeforeAmendment`, `dueDay`; e as parcelas: `number`, `dueDate`, `value`, `paid`, `status` (`PAID`, `OVERDUE`, `NEXT`, `UPCOMING`, `REPLACED`, a substituída pelo aditivo) |
| Conta · Sessões | `GET /api/billing/contracts/{id}/sessions` | `hasBeneficiary`; por atendimento ativo (R37): `consultationId`, `occurredAt`, `techniqueName`, `durationMinutes`, `patientName`, `providerName`, `status`, `baseValue`, `number`, `contractedCount` ("7ª de 6"), `tag` (`BEYOND_CONTRACT`, `BEYOND_CLASSES`, `OUT_OF_CONTRACT`, `REMOVED_ITEM` ou nenhuma) |
| Conta · Pagamentos | `GET /api/payments?patientId&contractId` | por pagamento: `id`, `contractId`, `contractLabel` ("Evolução · Aditivo 1" ou "Avulso"), `amount` (bruto), `netAmount`, `fee` (valor e %), `paymentMethod`, `cardInstallments`, `paidAt`, `active` |
| Registrar pagamento | `POST /api/payments/preview` (nada é gravado; precedente: as prévias do contrato) | recebe bruto, líquido, forma, "parcelado em" e data; devolve `fee` (valor e %) e `receivables` (a previsão de recebimento, R26: data e valor de cada parte). As sugestões de valor já vêm na conta (`paymentSuggestions`) |
| Registrar pagamento | `POST /api/payments` | recebe `patientId`, `contractId` (opcional), `amount`, `netAmount`, `cardInstallments`, `paidAt`, `paymentMethod`; 201 com o pagamento |
| Inativar pagamento | `DELETE /api/payments/{id}` | 204 |
| Dashboard · Produção | `GET /api/billing/production?from&to` | o total e a divisão: itens por duração, de-para, avulso e mensalidades do Grupo (R38). O formato final, com o dev dos dashboards |
| Dashboard · Faturamento | `GET /api/billing/revenue?from&to` | bruto, taxas e líquido, no total e por forma de pagamento; o "a receber" mês a mês (R26). O formato final, com o dev dos dashboards |

**Mensagens novas** (a regra de cada uma na seção 3):

| Código | Mensagem |
|---|---|
| 400 | "O valor do pagamento deve ser maior que zero." |
| 400 | "O valor líquido deve ser de R$ 0,00 até o valor pago." |
| 400 | "Só o cartão de crédito pode ser parcelado." |
| 400 | "O cartão pode ser parcelado de 1 a 12 vezes." |
| 400 | "A data do pagamento não pode ser futura." |
| 400 | "O contrato não pertence a este paciente." |
| 400 | "Forma de pagamento não aceita: use Pix, cartão de crédito, cartão de débito ou dinheiro." (R25) |
| 400 | "O 1º vencimento é obrigatório." |
| 400 | "O 1º vencimento deve ser entre o início do contrato e um mês depois." |
| 400 | "No aditivo, o 1º vencimento deve ser a partir da data do aditivo." |
| 404 | "Paciente não encontrado", "Contrato não encontrado", "Pagamento não encontrado" (o padrão dos outros módulos, com o id) |
| 409 | "O pagamento já está inativo." |

As mensagens que já existem continuam (ex: as parcelas acima do máximo, agora pelo máximo da R20).

**A conta, calculada na leitura** (nunca gravada; por contrato, pela cadeia de versões):

| Item | Conta |
|---|---|
| Consumido | Σ `baseValue` dos atendimentos ativos: caso 1 em item por duração (`sessionDurationPriceId` preenchido) e caso 2 (de-para) |
| Mensalidades do Grupo vencidas | valor do item do Grupo ÷ meses do contrato (`Installments.maxFor`), × os meses já vencidos (pelos vencimentos) |
| Devido | consumido + mensalidades vencidas |
| Pago | Σ pagamentos ativos |
| Saldo | pago − devido (positivo = saldo credor; negativo = saldo devedor) |
| Parcelas | vencimento, valor e situação (paga, parcial, em aberto, atrasada): o pago da versão distribuído pela ordem de vencimento |
| Avisos | parcela vencida em aberto; devido acima do pago; sessões além das contratadas |

O pago que vale para as parcelas da versão atual é o pago da cadeia − o pago antes do aditivo
(total da versão − `installmentBase`), limitado entre 0 e o `installmentBase` (R23). Depois do
`deactivatedOn` do contrato, nada vence (R19). No **avulso**: devido = Σ `baseValue` dos atendimentos do
caso 3 do paciente; pago = Σ pagamentos dele sem contrato.

**Módulo `contract`**

| Arquivo | Mudança |
|---|---|
| `domain/ContractEntity` | `firstDueDate` (obrigatório; R21: no cadastro, do início até 1 mês depois; no aditivo, a partir da data do aditivo) e `installmentBase` (`Money`, R13). `create`: `installmentBase` = total. `amend(..., firstDueDate, paidSoFar)`: `installmentBase` = total − pago, mínimo 0 (R22); máximo de parcelas pelos meses que faltam (R20). `deactivatedOn` (R19), gravado no `deactivate()` |
| `domain/ContractItemValue` (**pré-requisito**, numa branch `fix/` antes do billing) | o item do Grupo pelas semanas do **contrato** × vezes por semana (R17); a prévia e o aditivo passam as semanas da vigência |
| `domain/Installments` | o cronograma: vencimento de cada parcela (1º vencimento + N meses, R11) e a distribuição do pago pela ordem de vencimento (situação de cada parcela). O `billing` usa daqui: uma regra num lugar só |
| `service/PaidAmountProvider` (novo) | a interface que o `billing` implementa |
| `service/ContractService` | no aditivo e na prévia do aditivo, o pago pelo `PaidAmountProvider` |
| `dto/` | `firstDueDate` no pedido do cadastro e do aditivo; as prévias trazem as parcelas com vencimento; a prévia do aditivo traz também o pago até agora, o `installmentBase`, o máximo de parcelas (R20) e o 1º vencimento sugerido (R21); a resposta do contrato traz `firstDueDate`, `installmentBase`, `deactivatedOn` e o cronograma (vencimento e valor de cada parcela, sem a situação, R39) |

**Módulo `consultation`**

| Arquivo | Mudança |
|---|---|
| `service/ConsultationService` | `findSessionBalance` também com o item do Grupo (R2), marcado como do Grupo na resposta: aulas contratadas (semanas do contrato × vezes por semana, R17) − lançadas. O `launch-options` continua sem saldo no Grupo (sem aviso na tela de Lançamento) |
| `repository/ConsultationRepository` | para o `billing`: o consumido pela cadeia de versões e o avulso do paciente (somas agrupadas, R40), os atendimentos ativos do contrato para a aba Sessões (R37) e o consumo por período para a Produção (R38) |

**Banco** (V1 editada, [ADR 0028](../adr/0028-migrations-editaveis-ate-versao-estavel.md): quem puxar
zera o banco local):

- tabela `payment` (FKs para `patient`, `contract` nullable e `app_user`; `amount` `DECIMAL(10,2)`
  com `CHECK > 0`; `net_amount` `DECIMAL(10,2)` com `CHECK` de 0 ao `amount`; `card_installments`
  com `CHECK` de 1 a 12; `paid_at`; `payment_method`; `active`);
- em `contract`: `first_due_date` (`DATE NOT NULL`), `installment_base` (`DECIMAL(10,2) NOT NULL`,
  `CHECK >= 0`) e `deactivated_on` (`DATE`, nullable; R19).

## 5. Desenho do frontend

Estrutura aprovada em 2026-11-26 (parte 2b da análise); o visual sai das prévias, antes do código.

**Telas:**

- **Menu:** item "Financeiro" (`/billing`), só `ADMIN`, depois de "Contratos".
- **Financeiro** (lista de contas, R12): padrão das listagens (`DataTable`, clique na linha, linha
  aberta marcada). Colunas: paciente, devido, pago, saldo, situação (Em dia / Atrasado / Com
  crédito) e avisos. Filtros **Todos / Devendo / Atrasados / Com crédito** (abre em Todos) e busca
  por nome.
- **Conta do paciente:** modal com abas (`Modal size="medium"`, como o detalhe do contrato), aberto
  pelo id:
  - **Resumo:** um bloco por contrato e um do avulso: devido, pago, saldo, avisos, o saldo de
    sessões dos itens e o de aulas do Grupo;
  - **Parcelas:** de cada contrato, com vencimento, valor e situação;
  - **Pagamentos:** a lista, com "Inativar" (confirmação), e "Registrar pagamento".
- **Registrar pagamento** (modal pequeno; **só pelo Financeiro**): contrato (os do paciente e "Sem
  contrato (avulso)"), valor (o em aberto do escolhido, editável: R6), data (hoje) e forma de
  pagamento.
- **Contratos:** "1º vencimento" no cadastro, com as datas das parcelas no rodapé da prévia; no
  aditivo, o 1º vencimento sugerido pela prévia e a revisão com o pago descontado ("Já pago:
  R$ 668,00") e as novas parcelas com as datas; no detalhe, as parcelas com vencimento e o link
  "Ver conta" (os pagamentos ficam só no Financeiro).

**Escolhas das prévias (2026-11-26):**

- **Lista: versão C.** Colunas paciente (com os contratos), saldo (com "pago X de Y"), próximo
  vencimento, situação e avisos; filtros com a contagem. Situação em quatro estados: Em dia,
  Devendo (usou mais do que pagou, sem parcela vencida), Atrasado (parcela vencida em aberto) e Com
  crédito.
  - **Parcelas vencidas:** a coluna mostra a mais antiga, quantas são e o total em atraso ("05/10 ·
    2 vencidas", "R$ 1.336,00 em atraso"); sem vencida, o próximo vencimento e o valor.
  - **Ordem:** os atrasados primeiro, da parcela vencida mais antiga para a mais recente; depois os
    outros.
- **Conta: versão B.** O saldo em destaque ("Deve R$ 334,00"), o consumido e o pago embaixo, as
  sessões no Resumo. Quatro abas, cada uma com um assunto:
  - **Resumo:** um cartão do contrato (plano, versão, vigência) com **quatro células iguais: total
    do contrato, pago, consumido e saldo**, cada uma com rótulo, valor e uma nota ("em 3
    parcelas", "44% do total", "10 sessões", "devendo"). O saldo tem o mesmo tamanho dos outros;
    só a cor muda (vermelho negativo, azul positivo). Da parcela, só uma linha de atalho
    ("Parcelas: 1 atrasada · próxima 10/12 · Ver parcelas"; "em dia" quando não há atraso). As
    **sessões**, um item por vez: o nome como subtítulo (no estilo do cabeçalho das tabelas) e,
    acima da barra, o usado no início ("2 · R$ 334,00") e o contratado no fim ("12 · R$ 2.004,00").
  - **Sessão além do contratado:** o trilho verde fica cheio até o contratado ("6 · R$ 1.002,00"),
    e o que passou sai dele numa extensão laranja, com a quantidade e o valor ("+2 · R$ 334,00").
    *Descartadas:* a barra inteira laranja com etiqueta; a barra cheia com uma linha de aviso.
  - **Parcelas:** tudo das parcelas, sem gráfico. O painel tem altura fixa: **cada tabela rola por
    dentro, com o cabeçalho das colunas parado** (como as listagens). Sem aditivo, a tabela ocupa o
    painel e as 12 parcelas cabem; com aditivo, os blocos dividem o espaço na proporção do
    conteúdo, e cada lista rola. No topo, o resumo em texto ("6 de 12 pagas · próxima 15/12 ·
    R$ 723,66"). Embaixo, um bloco por versão do contrato: o valor parcelado e de onde vem ("R$ 2.338,00 em 3x: total
    R$ 3.006,00 − R$ 668,00 já pagos antes do aditivo"), o dia do vencimento e a tabela (parcela,
    vencimento, valor, pago, situação: "Atrasada · R$ 111,34 em aberto"). As versões anteriores ficam
    tracejadas, com as parcelas substituídas pelo aditivo apagadas.
  - **Sessões:** listagem dos atendimentos ativos do contrato (data, técnica, sessão "5ª de 6",
    status, prestador, valor; e "Paciente", quando o contrato tem beneficiário, R33), com filtro por
    técnica, a tabela rolando por dentro e o cabeçalho parado. A sessão além do contratado fica com
    a linha destacada e a etiqueta "8ª de 6 · além do contratado"; as outras etiquetas da R37
    ("além das aulas previstas", "fora do contrato", "item removido no aditivo"). Sem totais nem
    notas soltas: cada linha já diz o que é.
  - **Pagamentos:** listagem (a tabela rola por dentro, cabeçalho parado). Colunas: data (com o
    contrato embaixo), forma (com "à vista (1x)" ou "3x" no crédito), bruto, taxa (valor e %),
    líquido, **status** (Ativo, Inativado) e **ações** ("Inativar", com confirmação; cabeçalho
    "Ações", como nas listagens). O inativado fica
    apagado, com a data e o bruto riscados, sem o botão.
  - **Registrar pagamento:** contrato; "o que está sendo pago" (parcela em aberto, todas as
    parcelas em aberto ou outro valor: cada opção sugere o valor); forma; "parcelado em" (só no
    crédito); valor pago (bruto) e valor recebido (líquido, preenchido com o bruto); data; e a
    conferência: a taxa (valor e %) e a previsão de recebimento do cartão ("3 × R$ 534,40, dez,
    jan e fev").
  - **Sessão além do contratado:** a barra do item passa do limite numa cor de aviso, com "8 de 6 ·
    2 a mais" e o valor a mais.

**Módulo:** `domain/billing/` (`api/billingApi.js` e hooks, `pages/BillingPage`, `components/`),
acima do `consultation` no mapa de módulos do front, como no backend.

## 6. Tarefas

**Análise:**
- [x] Parte 1: escopo e regras de negócio (seção 3).
- [x] Parte 2a: desenho do backend (seção 4).
- [x] Parte 2b: estrutura das telas (seção 5).
- [x] Prévias do Financeiro, da conta do paciente (as quatro abas) e do registrar pagamento,
  aprovadas em 2026-11-26 (seção 5).
- [x] Revisão das regras, em três partes (R17 a R40).
- [x] Seção 4 conferida contra as regras: endpoints e respostas de cada tela e as mensagens.
- [x] Casos de teste (seção 7), blocos A a J.
- [x] Tarefas de implementação (abaixo).

A implementação vai em três branches, cada uma com o próprio PR para a `dev` (merge commit), na
ordem das dependências (precedente: o aditivo, backend e front em PRs separados). Cada etapa
termina com o `./mvnw -q compile` (e o `npm run build` e `npm run lint`, no front) e um resumo curto.

**Etapa 0, a correção das aulas do Grupo** (branch `fix/aulas-do-grupo-pela-vigencia`, antes do
billing; R17):
- [ ] `ContractItemValue`: o item do Grupo pelas semanas do contrato × vezes por semana; quem chama
  (o item salvo, o rascunho da prévia, o aditivo) passa as semanas da vigência.
- [ ] Testes de unidade: casos A1 a A5 (no `ContractEntity...Test` que já existe ou num novo).
- [ ] Documentação da correção: `domain-model.md` ("Valor e parcelas", "Plano do item do Grupo"); o
  ADR das aulas pela vigência (substituindo em parte o 0032); a descrição do PR com o aviso de que
  muda o valor dos contratos mistos.

**Etapa 1, o contrato com vencimento** (branch `feature/billing`; R4, R11, R13, R19 a R23):
- [ ] V1: as colunas `first_due_date`, `installment_base` e `deactivated_on` em `contract`.
- [ ] `Installments`: o cronograma (o 1º vencimento + N meses, R11), a mensalidade do Grupo (R18)
  e a distribuição do pago pela ordem de vencimento, com os limites da R23. Testes de unidade.
- [ ] `ContractEntity`: `firstDueDate` (validação da R21, no cadastro e no aditivo),
  `installmentBase` (R13, mínimo 0, R22), `deactivatedOn` (no `deactivate()` e, na versão
  substituída, o `createdAt` da sucessora, R19), o máximo de parcelas no aditivo (R20). Testes de
  unidade (casos B1 a B15).
- [ ] `PaidAmountProvider` (interface no `contract`) e o `ContractService` usando-a no aditivo e
  na prévia do aditivo (o pago até agora e o 1º vencimento sugerido, R21).
- [ ] DTOs do contrato: o `firstDueDate` nos pedidos; as parcelas com vencimento nas prévias; a
  resposta com `firstDueDate`, `installmentBase`, `deactivatedOn` e o cronograma (R39). Swagger só
  com o básico.

**Etapa 2, o pagamento** (`feature/billing`; R3, R7, R10, R14, R15, R24 a R29):
- [ ] V1: a tabela `payment`, com os `CHECK`.
- [ ] `PaymentEntity` (validações em `validateX`/`requireX`, `deactivate()`), `PaymentRepository`
  (as somas pela cadeia e do avulso), `PaymentService`, `PaymentController` (registrar, prévia,
  listar, inativar).
- [ ] A prévia do pagamento: a taxa e a previsão de recebimento (R26, os centavos na primeira, o dia
  pela R11).
- [ ] `BillingPaidAmountProvider` (implementa o `PaidAmountProvider`).
- [ ] Testes de unidade da entidade e da previsão (casos C1 a C15).

**Etapa 3, o consumo no `consultation`** (`feature/billing`; R2, R17, R37, R40):
- [ ] `findSessionBalance` com o item do Grupo, marcado na resposta (o `launch-options` não muda).
- [ ] `ConsultationRepository`: o consumido pela cadeia de versões e o avulso (somas agrupadas), os
  atendimentos ativos do contrato (para a aba Sessões) e o consumo por período (para a Produção).

**Etapa 4, a conta e a lista** (`feature/billing`; R1, R2, R9, R18, R27, R30 a R37, R40):
- [ ] `BillingAccountService`: a conta do paciente (R32, R33), as parcelas com a situação (R23), as
  sessões numeradas (R37), as sugestões do pagamento (R27) e a lista (R34 a R36), com somas
  agrupadas (R40).
- [ ] `BillingController`: `accounts`, `patients/{id}/account`, `contracts/{id}/installments`,
  `contracts/{id}/sessions`.
- [ ] Testes de unidade do cálculo da conta (casos D1 a D18, E1 a E5, F1 a F7, G2 a G6) e um teste
  de integração do número de consultas da lista (G7).

**Etapa 5, o dashboard** (`feature/billing`; R16, R26, R38):
- [ ] Combinar com o dev dos dashboards o formato de `production` e `revenue`.
- [ ] Os dois endpoints; testes (casos H1 a H3).

**Etapa 6, o fechamento do backend** (`feature/billing`):
- [ ] Teste de integração da autorização (I1, I2).
- [ ] Os casos dos blocos B a I pela API, por script fora do repositório, com o banco zerado.
- [ ] A spec atualizada (o que foi feito, o que mudou) e a descrição do PR: zerar o banco local
  (V1 editada); as mudanças na API do contrato (`firstDueDate` obrigatório, a resposta nova).

**Etapa 7, as telas** (branch `feature/billing-front`, da `dev` depois do merge do backend; seção 5):
- [ ] Contratos: "1º vencimento" no cadastro, com as datas no rodapé; no aditivo, o sugerido, o
  "Já pago" e as novas parcelas com as datas; no detalhe, as parcelas com vencimento e o "Ver
  conta"; o saldo de aulas do Grupo nos itens do aditivo.
- [ ] Módulo `domain/billing`: a camada `api/` (`billingApi.js`, `paymentApi.js` e os hooks do
  TanStack Query) e a rota `/billing` com o item "Financeiro" no menu (só `ADMIN`).
- [ ] A lista do Financeiro (versão C, na `DataTable`).
- [ ] A conta do paciente: o modal com as quatro abas (Resumo, Parcelas, Sessões, Pagamentos).
- [ ] O registrar pagamento (com a prévia) e o inativar pagamento (com confirmação).
- [ ] O bloco J pela tela; `npm run build` e `npm run lint`.

**Etapa 8, a conclusão** (no PR das telas):
- [ ] As regras para o `domain-model.md`; o `billing` no mapa de módulos do `architecture.md` (o
  backend e o front) e o padrão do `PaidAmountProvider` (a inversão de dependência); os ADRs da
  seção 10; o `roadmap.md`; esta spec removida (seção 9); `node scripts/check-docs.mjs` e a lista
  do "Ao concluir uma tarefa" do `CLAUDE.md`.

## 7. Casos de teste

Escritos em 2026-11-26, a partir das regras R1 a R40. Pela API (Swagger ou script fora do
repositório) e, no bloco J, pela tela.

**Pré-requisitos:**
- banco zerado (a V1 editada), com o catálogo da V4 e o admin padrão; um `PROVIDER` com prestador;
- preços do seed usados nos casos: RPG 60 min no Evolução **R$ 167,00** (Individual Padrão);
  Quiropraxia 30 min no Evolução **R$ 132,00** (Individual Especializado); Avulso 60 min no
  Individual Padrão **R$ 220,00**; aula do Grupo Trimestral 2x **R$ 65,81**;
- **H** = hoje (no fuso da clínica). Contratos com início no passado e lançamentos com data passada
  são feitos pelo admin (lança qualquer data não futura);
- pacientes: **Ana** (titular), **João** (beneficiário da Ana no caso D7), **Diego** (sem contrato),
  **Marina**, **Rafael** e **Bia** (para a lista).

**Contrato base da Ana (C-Ana):** Evolução, RPG 60 min semanal = 12 × R$ 167,00 = **R$ 2.004,00**,
12 semanas → 3 meses, **3 parcelas de R$ 668,00**, início em H − 70 dias, 1º vencimento = o início.

**A. Aulas do Grupo pela vigência (R17; na branch `fix/`, antes do billing)**

| # | Caso | Esperado |
|---|---|---|
| A1 | Prévia: Evolução, RPG 60 min 2x/semana + Grupo Trimestral 2x | vigência 6 semanas; Grupo **12 aulas** (6 × 2) × R$ 65,81 = **R$ 789,72** (hoje: 26 aulas, R$ 1.711,06) |
| A2 | Contrato só Trimestral 2x | 13 × 2 = 26 aulas, R$ 1.711,06 (sem mudança) |
| A3 | Trimestral 2x + RPG 60 min quinzenal (pacote equivalente: Evolução, 12 sessões × 2 semanas) | vigência 24 semanas; Grupo 24 × 2 = 48 aulas × R$ 65,81 = **R$ 3.158,88** |
| A4 | Aditivo prorroga um contrato misto em 4 semanas | o Grupo ganha 4 × a frequência em aulas, ao preço travado; o total e as parcelas da nova versão refletem isso |
| A5 | Aditivo sem nenhuma alteração num contrato misto | 400 "Nenhuma alteração no contrato." (a regra do aditivo continua) |

**B. Contrato: vencimento, valor parcelado e inativação (R4, R11, R13, R19 a R22)**

| # | Caso | Esperado |
|---|---|---|
| B1 | Cadastrar sem `firstDueDate` | 400 "O 1º vencimento é obrigatório." |
| B2 | `firstDueDate` um dia antes do início | 400 "O 1º vencimento deve ser entre o início do contrato e um mês depois." |
| B3 | `firstDueDate` = início + 1 mês + 1 dia | 400, a mesma mensagem |
| B4 | C-Ana (`firstDueDate` = início) | 201; `installmentBase` R$ 2.004,00; cronograma: 3 parcelas de R$ 668,00, nos meses seguidos, no dia do início; `deactivatedOn` nulo |
| B5 | `firstDueDate` em 31/01, 3 parcelas | vencimentos 31/01, 28/02 (29/02 em ano bissexto) e **31/03** (não 28/03: R11) |
| B6 | Inativar o C-Ana | 204; o `deactivatedOn` passa a ser H |
| B7 | Prévia do aditivo do C-Ana em H, com R$ 668,00 pagos, incluindo Quiropraxia 30 min semanal (12 × R$ 132,00 = R$ 1.584,00) | total R$ 3.588,00; pago até agora R$ 668,00; `installmentBase` **R$ 2.920,00**; máximo de parcelas = os meses do novo 1º vencimento até o novo fim (R20); o 1º vencimento sugerido pela R21 |
| B8 | Aditivo com `firstDueDate` antes de H | 400 "No aditivo, o 1º vencimento deve ser a partir da data do aditivo." |
| B9 | Aditivo com parcelas acima do máximo da R20 | 400 "O número de parcelas deve ser de 1 a N (meses do contrato).", com o N da R20 |
| B10 | Aditivo de um contrato de 12 meses no 10º mês | o máximo de parcelas é 2 (não 12) |
| B11 | Aditivo em que o pago cobre o total novo (ex: remove um item depois de quase tudo pago) | `installmentBase` R$ 0,00; a prévia avisa "O pago cobre o total: nada a parcelar"; 1 parcela de R$ 0,00, paga; o excedente como saldo credor (R22) |
| B12 | Depois do aditivo, ler a versão anterior | `deactivatedOn` = o `createdAt` da nova versão |
| B13 | Sugestão do 1º vencimento (R21): a parcela deste mês **paga** | o mesmo dia no mês seguinte |
| B14 | Sugestão: a parcela deste mês **não paga** e o dia ainda não passou | o dia deste mês |
| B15 | Sugestão: não paga e o dia já passou | o dia do mês seguinte |

**C. Pagamento: registro, validações e prévia (R3, R7, R10, R14, R15, R24 a R29)**

| # | Caso | Esperado |
|---|---|---|
| C1 | Pagar R$ 668,00 em Pix no C-Ana, líquido R$ 668,00, data H | 201; `registeredByUser` = o admin |
| C2 | `amount` 0 | 400 "O valor do pagamento deve ser maior que zero." |
| C3 | `netAmount` maior que o `amount`, ou negativo | 400 "O valor líquido deve ser de R$ 0,00 até o valor pago." |
| C4 | `cardInstallments` 3 com Pix | 400 "Só o cartão de crédito pode ser parcelado." |
| C5 | `cardInstallments` 13 no crédito | 400 "O cartão pode ser parcelado de 1 a 12 vezes." |
| C6 | `paidAt` = H + 1 | 400 "A data do pagamento não pode ser futura." |
| C7 | O contrato da Ana com o `patientId` do Diego | 400 "O contrato não pertence a este paciente." |
| C8 | Pagar num contrato inativado | 201 (R10) |
| C9 | Pagar sem contrato (avulso) | 201 |
| C10 | `paymentMethod` = `BANK_SLIP` | 400 "Forma de pagamento não aceita: use Pix, cartão de crédito, cartão de débito ou dinheiro." |
| C11 | Inativar um pagamento | 204; de novo: 409 "O pagamento já está inativo."; id inexistente: 404 |
| C12 | Prévia: crédito 3x, bruto R$ 1.670,00, líquido R$ 1.603,20 | taxa **R$ 66,80 · 4,0%**; previsão 3 × **R$ 534,40**, na data + 1, + 2 e + 3 meses |
| C13 | Prévia: líquido R$ 1.603,21 | previsão R$ 534,41 + 2 × R$ 534,40 (os centavos na primeira) |
| C14 | Prévia: Pix | taxa R$ 0,00 (líquido = bruto); previsão: 1 parte, na data do pagamento |
| C15 | Prévia: crédito 1x em 31/01 | previsão em 28/02 (ou 29/02): o dia pela R11 |

**D. A conta (R1, R2, R9, R18, R23, R30 a R33)**

| # | Caso | Esperado |
|---|---|---|
| D1 | C-Ana: 4 sessões lançadas, R$ 668,00 pago | consumido R$ 668,00; saldo R$ 0,00; `status` `UP_TO_DATE` |
| D2 | 6 sessões, R$ 668,00 pago | consumido R$ 1.002,00; saldo **− R$ 334,00**; `OWING` |
| D3 | Uma sessão `NO_SHOW` e uma `CANCELED_LATE` | as duas somam no consumido (os três status são cobrados) |
| D4 | Inativar um atendimento | sai do consumido |
| D5 | 14 sessões no RPG (12 contratadas) | o item mostra excesso 2 × R$ 167,00 = R$ 334,00; aviso "RPG: 2 sessões além das contratadas" |
| D6 | A Ana faz Quiropraxia 30 min, fora do contrato (de-para: Evolução no Individual Especializado) | + **R$ 132,00** no consumido do C-Ana (caso 2, R1) |
| D7 | O João, beneficiário, é atendido pelo contrato da Ana | soma na conta da Ana (R9); a conta do João não mostra esse atendimento (R33) |
| D8 | Contrato só Trimestral 2x (R$ 1.711,06, 3 meses), 1º vencimento em H − 35 dias, 10 aulas lançadas | mensalidades R$ 570,36 + 2 × R$ 570,35 (R18); 2 vencidas → devido **R$ 1.140,71**; as aulas **não** somam no consumido; o saldo de aulas: 26 − 10 = 16 |
| D9 | Contrato misto (Evolução RPG + Grupo) | devido = o consumo do RPG + as mensalidades vencidas do Grupo |
| D10 | Contrato só Grupo, pago à vista (1 parcela), mês a mês | o saldo credor diminui a cada mensalidade vencida (R18) |
| D11 | C-Ana inativado no meio, com saldo devedor | as parcelas depois do `deactivatedOn` não ficam `OVERDUE`; o Grupo (se houver) para de somar; o contrato continua na conta enquanto o saldo ≠ 0 (R32) |
| D12 | Contrato inativado com saldo 0 | não aparece na conta (R32) |
| D13 | Aditivo do B7 feito; o pagamento de R$ 668,00 (de antes) é inativado | as parcelas do aditivo continuam somando R$ 2.920,00; os R$ 668,00 voltam como saldo devedor (R30) |
| D14 | Pagamento retroativo acima do valor parcelado | as parcelas não passam de pagas (R23); o excedente é saldo credor |
| D15 | Diego atendido sem contrato (Avulso 60 min) | `singleSession`: devido **R$ 220,00**; sugestão "Débito em aberto" R$ 220,00; pago R$ 220,00 sem contrato → saldo 0 |
| D16 | Sugestões do C-Ana com a 1ª parcela vencida e parcial (pago R$ 556,66 de R$ 668,00) | "Parcela em aberto" R$ 111,34; "Todas as parcelas em aberto" R$ 1.447,34 |
| D17 | Marina: parcelas quitadas, 2 sessões além (R$ 334,00) | só "Saldo devedor" R$ 334,00 (e "Outro valor"): as parcelas não têm nada em aberto |
| D18 | Paciente sem contrato nem avulso | 200, sem contratos e sem avulso |

**E. Aba Parcelas (R13, R15, R22, R23)**

| # | Caso | Esperado |
|---|---|---|
| E1 | C-Ana na 2ª parcela, sem pagar a 1ª (vencida) | 1ª `OVERDUE`, 2ª `NEXT` (ou `OVERDUE`, se venceu), 3ª `UPCOMING`; o pago de cada uma |
| E2 | Pagar R$ 1.000,00 | 1ª `PAID`, 2ª com R$ 332,00 pagos (distribuído pela ordem de vencimento) |
| E3 | Depois do aditivo do B7 | duas versões, a mais nova primeiro; na original, as parcelas não pagas `REPLACED`; `paidBeforeAmendment` R$ 668,00; `installmentBase` R$ 2.920,00 |
| E4 | Pagar "Todas as parcelas em aberto" no crédito 3x | todas as parcelas da versão `PAID` na hora (R15) |
| E5 | Versão do B11 (`installmentBase` 0) | 1 parcela de R$ 0,00, `PAID` |

**F. Aba Sessões (R33, R37)**

| # | Caso | Esperado |
|---|---|---|
| F1 | 3 sessões de RPG lançadas fora de ordem (datas H − 10, H − 20, H − 15) | numeradas pela data: 1ª (H − 20), 2ª (H − 15), 3ª (H − 10); no mesmo dia, pela ordem do lançamento |
| F2 | 13ª sessão de RPG (12 contratadas) | "13ª de 12" com `BEYOND_CONTRACT` |
| F3 | Grupo: aula acima das contratadas | `BEYOND_CLASSES`, sem valor a mais na conta |
| F4 | Atendimento do de-para (D6) | sem número, `OUT_OF_CONTRACT` |
| F5 | Item removido por aditivo | as sessões dele continuam, com `REMOVED_ITEM` |
| F6 | Inativar a 1ª sessão | sai da lista; as seguintes viram 1ª e 2ª |
| F7 | Contrato com beneficiário (D7) | `hasBeneficiary` verdadeiro; `patientName` de cada atendimento |

**G. Lista do Financeiro (R34 a R36, R40)**

| # | Caso | Esperado |
|---|---|---|
| G1 | Quem aparece | a Ana (contrato), o Diego (só avulso); um paciente inativado com saldo ≠ 0 aparece; um paciente sem contrato e sem avulso não aparece |
| G2 | Prioridade da situação | com parcela vencida: `LATE` (mesmo com saldo credor: pagou R$ 300,00, consumiu R$ 0,00, a parcela de R$ 668,00 venceu); sem vencida e saldo negativo: `OWING`; positivo: `CREDIT`; zero: `UP_TO_DATE` |
| G3 | Duas parcelas vencidas | `overdueCount` 2, `overdueAmount` a soma, `oldestOverdueDate` a mais antiga |
| G4 | Ordem | os `LATE` pela parcela vencida mais antiga; depois os `OWING` pelo maior saldo devedor; depois os outros por nome |
| G5 | Filtros (pelos campos) | Devendo: `balance` < 0 (inclui os atrasados que devem); Atrasados: `hasOverdue`; Com crédito: `balance` > 0; o paciente do G2 (atrasado com saldo credor) está em Atrasados e em Com crédito |
| G6 | Paciente com o C-Ana e um avulso em aberto | `balance` = a soma das duas contas; a situação pela prioridade, conta a conta |
| G7 | Desempenho | o número de consultas ao banco não cresce com o número de pacientes (log do SQL ou teste de integração) |

**H. Dashboard (R16, R26, R38)**

| # | Caso | Esperado |
|---|---|---|
| H1 | Produção do mês | o consumo (duração, de-para, avulso) pela data do atendimento + as mensalidades do Grupo pelo vencimento; as aulas do Grupo não somam; igual à soma do devido das contas no mesmo período |
| H2 | Faturamento do mês | bruto, taxas e líquido dos pagamentos **ativos**, pela data do pagamento, no total e por forma |
| H3 | "A receber" com o crédito 3x do C12 | R$ 534,40 em cada um dos 3 meses seguintes |

**I. Autorização (R7)**

| # | Caso | Esperado |
|---|---|---|
| I1 | `PROVIDER` em qualquer rota de `/api/payments` e `/api/billing` | 403 |
| I2 | Sem token | 401 |

**J. Tela**

| # | Caso | Esperado |
|---|---|---|
| J1 | Menu | "Financeiro" depois de "Contratos", só para o `ADMIN` |
| J2 | Lista | as colunas da versão C, os filtros com a contagem, a ordem do G4; clique na linha abre a conta, e a linha fica marcada |
| J3 | Conta · Resumo | as 4 células (total, pago, consumido, saldo), o saldo em vermelho (negativo) ou azul (positivo) no mesmo tamanho; a linha de atalho das parcelas; as barras com o usado e o contratado; a sessão a mais como extensão laranja |
| J4 | Conta · Parcelas | uma tabela por versão, cada uma rolando por dentro com o cabeçalho parado; sem aditivo, as 12 parcelas cabem |
| J5 | Conta · Sessões e Pagamentos | as etiquetas da R37; "Inativar" com confirmação, e o pagamento inativado apagado, sem o botão |
| J6 | Registrar pagamento | as sugestões só as que valem (R27); "Parcelado em" só no crédito; a taxa e a previsão pela prévia; aviso quando o valor passa do maior em aberto (R28) |
| J7 | Contratos | "1º vencimento" no cadastro, com as datas no rodapé; no aditivo, o sugerido, o "Já pago" e as novas parcelas com as datas; no detalhe, as parcelas com vencimento e valor e o link "Ver conta" |
| J8 | Saldo de aulas | o do Grupo aparece no aditivo; a tela de Lançamento continua sem aviso no Grupo |

## 8. Pendências e pontos abertos

- *Em aberto (técnico, decidir na implementação):* **mensalidades do Grupo com aditivo** que
  inclui ou remove o item do Grupo, ou muda o plano dele: as mensalidades vencidas são contadas
  pela versão de cada período. Proposta: cada versão conta os meses entre o próprio `createdAt` e o
  da sucessora (o `deactivatedOn` dela, R19).
- *Para combinar:* as visões do dashboard (R16) com o dev da feature de dashboards: o billing
  oferece os endpoints; a tela é dele.
- *Futuro (fora desta feature):* **integração com um gateway de pagamento** (ex: Asaas). O desenho
  não a bloqueia: bruto e líquido separados, pagamento imutável (estorno = inativar), a previsão do
  cartão tratada como estimativa (passaria a vir da API) e o gateway como um adaptador dentro do
  `billing` (o domínio declara a interface, como o `PaidAmountProvider`). O que muda na integração:
  o fluxo inverte (a cobrança nasce antes, pendente, e o pagamento chega pelo webhook), e as
  parcelas, hoje calculadas, passam a ser gravadas como cobranças (`Charge`, com o id externo e o
  status), além de `source` e `externalId` no pagamento. Não antecipar: a `Charge` só faz sentido
  com o gateway.

## 9. Documentação ao concluir

- [`domain-model.md`](../domain-model.md): o módulo `billing` (`Payment` e a conta do paciente);
  o vencimento em `Contract` ("Valor e parcelas" e o aditivo); o saldo de aulas do Grupo em "Saldo
  de sessões" de `Consultation`.
- [`architecture.md`](../architecture.md): o `billing` no mapa de módulos.
- [`roadmap.md`](../roadmap.md): o billing concluído (sai de "Não modelado ainda", fica o "Saldo do
  paciente" com o encerramento); o que a seção 2 deixou para depois.
- Remover esta spec e a entrada em [`specs/README.md`](README.md).

## 10. ADRs a registrar

- **A conta do paciente pelo consumo, com as parcelas como referência** (R1 e R3). *Descartadas:*
  as parcelas como a dívida; o pagamento por parcela.
- **Pilates em Grupo com duas contas, dinheiro e aulas** (R2). *Descartadas:* sem saldo de aulas
  (a regra anterior); as aulas lançadas no consumido; a quantidade pelo pacote equivalente.
- **Parcelas do aditivo sobre o que falta pagar** (R5), substituindo em parte o
  [ADR 0037](../adr/0037-parcelas-escolhidas-no-aditivo.md). *Descartada:* dividir o total da nova
  versão.
- **O valor parcelado gravado por versão** (R13), substituindo em parte o
  [ADR 0032](../adr/0032-valor-e-parcelas-do-contrato.md). *Descartada:* recalcular as parcelas na
  leitura pelo pago.
- **O `contract` declara o `PaidAmountProvider`, o `billing` implementa** (inversão de
  dependência; seção 4). *Descartadas:* o aditivo no `billing`; o pago vindo do front.
- **As aulas do Grupo pela vigência do contrato** (R17), substituindo em parte o
  [ADR 0032](../adr/0032-valor-e-parcelas-do-contrato.md). *Descartada:* as semanas do plano do
  Grupo. Registrar na branch `fix/` da correção, não na do billing.
- **O máximo de parcelas no aditivo pelos meses que faltam** (R20), substituindo em parte o
  [ADR 0037](../adr/0037-parcelas-escolhidas-no-aditivo.md). *Descartada:* os meses da vigência
  inteira.
