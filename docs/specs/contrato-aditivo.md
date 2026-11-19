# Spec — Aditivo do contrato

Tela do aditivo: alterar um contrato ativo (vigente ou vencido) gerando uma nova versão
(prorrogar, incluir e remover itens, trocar beneficiário, forma de pagamento e parcelas), com
prévia do resultado antes de salvar.

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

*Atualizado em 2026-11-18.*

- **Branch:** `feature/contrato-aditivo`, a partir da `dev`; PR para a `dev` com "merge commit".
- **Feito:** a análise (parte 1): o que o backend já tem, as três lacunas e as decisões da
  seção 3. Nenhum código ainda.
- **Próximo:** a parte 2, o desenho da tela, com as **prévias visuais** antes do código (regra
  "Mudança visual ou de layout" do `CLAUDE.md`), e o formato exato dos dados novos da API
  (seções 4 e 5).
- **O que o backend já tem** (ver "Versionamento (aditivos)" em `Contract`, no
  [`domain-model.md`](../domain-model.md)):
  - `POST /api/contracts/{id}/amendments`: gera a nova versão com o estado completo (item que
    não vier é removido); titular, plano e início copiados; `endDate` opcional, só para
    estender. Hoje **copia as parcelas** (`ContractEntity.amend`).
  - `GET /api/contracts/{id}/group-plan-options`: opções do plano do Grupo pela vigência atual.
  - `GET /api/consultations/session-balance?contractId`: saldo de cada item por duração.
  - A resposta do contrato traz `previousContractId` (a versão anterior), mas não diz se a
    versão foi substituída nem o número da versão.
- **O que o frontend já tem:** a tela de Contratos com o cadastro (`ContractForm`,
  `ContractItemFields`, `useContractDraft`), o detalhe (`ContractDetailsModal`) e a prévia da
  criação. Hoje prorrogar só pelo Swagger.

## 2. Objetivo e escopo

O admin altera um contrato pela tela, vendo antes de salvar o novo fim, o valor de cada item, o
total e as parcelas. Depende de `contract` (backend e front) e lê o saldo de sessões do
`consultation`.

**Entra:**
- prorrogar (novo fim);
- incluir e remover itens, trocar técnica, linha de preço e frequência;
- trocar beneficiário e forma de pagamento;
- **escolher as parcelas** da nova versão (regra nova, R2);
- prévia do aditivo antes de salvar (endpoint novo, R1);
- o "Editar" no detalhe do contrato;
- a lista com **um contrato por linha** (a versão atual) e o histórico das versões no detalhe
  (R3);
- a correção da numeração do ADR dos testes de integração (seção 8), na mesma branch.

**Fica para depois:**
- o controle de pagamentos (`billing`, Fase 2; esboço na seção 8): o aditivo não sabe o que
  já foi pago;
- o destino das sessões restantes de um item removido (vai para o saldo do paciente, no
  `billing`);
- a impressão da versão para assinatura.

## 3. Regras

Decididas em 2026-11-18, ainda não implementadas.

**R1. Prévia do aditivo** (aprovada): `POST /api/contracts/{id}/amendments/preview`, no mesmo
formato do aditivo, sem gravar. Devolve o novo fim, o valor de cada item, o total, o máximo de
parcelas e as aplicadas.
- *Por quê:* o fim do aditivo segue outra regra que o da criação (mantém o fim atual, estende
  se pedido, ou recalcula a partir da data do aditivo quando entra um item por duração). Sem
  prévia, o admin só vê o resultado depois de salvar, e a correção é outro aditivo.
- *Descartadas:* reaproveitar o `POST /api/contracts/preview` da criação (calcula o fim pelo
  início); calcular no front (regra do backend).

**R2. Parcelas no aditivo** (aprovada): o admin escolhe o número de parcelas **em qualquer
aditivo**, de 1 até os meses da **nova** vigência (máximo 12), a mesma regra da criação
(`Installments.validateCount`). As parcelas dividem **o total da nova versão**. Mensagem acima
do máximo: a mesma da criação ("O número de parcelas deve ser de 1 a N (meses do contrato).").
- *Por quê:* as parcelas são só o **plano de pagamento**; a dívida do paciente é o consumo
  (esboço do `billing`, seção 8). Incluir ou remover um item muda o total sem mexer no fim, e
  prorrogar muda o máximo.
- *Descartadas:* copiar sempre as parcelas (hoje: o total muda e as parcelas não); renegociar
  só na prorrogação (incluir um item também muda o total); descontar as parcelas já pagas (o
  sistema não sabe o que foi pago até o `billing`).
- Substitui em parte o [ADR 0032](../adr/0032-valor-e-parcelas-do-contrato.md) ("Aditivo:
  copia as parcelas da versão anterior").

**R3. Lista com um contrato por linha** (aprovada): a lista mostra **só a versão atual** de cada
contrato, com a indicação do aditivo (ex: "Aditivo 2"); o histórico das versões fica no detalhe.
O status "Inativo" passa a ser só o contrato inativado.
- *Por quê:* hoje um contrato com dois aditivos vira três linhas, duas como "Inativo", iguais a
  um contrato encerrado.
- Precisa de dado novo do backend (formato na parte 2): o número da versão e se ela foi
  substituída. Montar isso no front cruzando a lista seria reimplementar a cadeia de versões.
- *Descartada:* todas as versões na lista, com a substituída como "Substituído".

## 4. Desenho do backend

*A desenhar na parte 2.* Já se sabe:

| Arquivo | Mudança |
|---|---|
| `ContractController` | `POST /api/contracts/{id}/amendments/preview` (R1); textos do `@Operation` do aditivo com as parcelas (R2) |
| `ContractAmendmentRequestDTO` | `installments` (R2) |
| `ContractEntity.amend` | recebe e valida as parcelas no lugar de copiá-las (R2) |
| `ContractService` | prévia do aditivo (R1); dados da versão na listagem (R3) |
| `ContractResponseDTO` (ou a listagem) | número da versão e versão substituída (R3) |

## 5. Desenho do frontend

*A desenhar na parte 2*, com as prévias visuais antes do código. Já se sabe: o "Editar" no
`ContractDetailsModal`; o formulário do aditivo aproveita o do cadastro (`ContractForm`,
`ContractItemFields`, `useContractDraft`), sem titular, plano e início editáveis; a lista com um
contrato por linha (R3).

## 6. Tarefas

**Desenho (parte 2):**
- [ ] Tela do aditivo: prévias visuais (2 ou 3 versões) e a escolhida.
- [ ] Formato da prévia do aditivo e dos dados da versão na API.
- [ ] Como a tela mostra o saldo de sessões (ponto aberto na seção 8).

**Backend:**
- [ ] Parcelas no aditivo (R2).
- [ ] Prévia do aditivo (R1).
- [ ] Dados da versão para a lista (R3).
- [ ] Testes de unidade das regras novas (parcelas no aditivo).

**Frontend:**
- [ ] "Editar" no detalhe e formulário do aditivo com a prévia.
- [ ] Lista com um contrato por linha e histórico das versões no detalhe.

**Correção na branch:**
- [ ] Numeração do ADR dos testes de integração (seção 8).

**Conclusão:** casos de teste (seção 7), documentação (seção 9), ADRs (seção 10).

## 7. Casos de teste

*A escrever na parte 2.*

## 8. Pendências e pontos abertos

**Esboço do `billing`** (Fase 2; decidido na análise de 2026-11-18). Vai para o
[`roadmap.md`](../roadmap.md) na conclusão, no lugar dos itens "`Payment`" e "Saldo do
paciente" de "Não modelado ainda":
- **Nome do módulo: `billing`**, não `payment`: cobre o que é cobrado e o que é pago.
  `Payment` é uma entidade dentro dele. Precedente de módulo com nome do assunto: o `pricing`.
- **A dívida do paciente é o consumo** (atendimentos lançados × valor), comparado com os
  pagamentos. É o "controle de atendimento" que a clínica faz hoje à mão: o paciente que vai
  mais que o contratado (ex: aumentou a frequência por uma dor nas costas) termina devendo, e
  quem desmarca com antecedência termina com crédito. O sistema já grava o consumo
  (`Consultation.baseValue`, saldo negativo aceito pelo ADR 0018); falta o pago.
- **O contrato** trava o preço, define a expectativa de uso (sessões e frequência) e o plano de
  pagamento (as parcelas). As parcelas **não** são a dívida.
- **Sessão além do contratado** vale o **valor do item do contrato** (a linha travada), como o
  lançamento já faz.
- **Pilates em Grupo:** mensalidade, sem controle de sessões: a dívida é a parcela do mês, não o
  consumo.
- **Avisos:** quando o consumo passa do contratado ou do que foi pago, para a clínica avisar o
  paciente cedo (antes de o valor virar 2x ou 3x o acordado).
- **Crédito no fim do contrato:** abater em novas sessões, levar para outro contrato ou
  estornar (o destino que o item "Saldo do paciente" já prevê).
- **Módulos:** o `billing` fica **acima** do `consultation` no mapa e **lê** o `contract` e o
  `consultation`, sem juntá-los: são agregados com regras e ciclos de vida diferentes
  (versionado, imutável). Precedente: o saldo de sessões é calculado no `consultation`, lendo o
  `contract`.
- A frase do roadmap "pagamentos realizados × valor consumido" estava certa; o "Saldo do
  paciente" se junta a ela no mesmo módulo.

**Saldo de sessões na tela do aditivo** (para a parte 2): o `session-balance` é do
`consultation`, e no front o módulo `contract` não pode importá-lo (mapa de módulos no
[`architecture.md`](../architecture.md)). Decidir onde a chamada mora.

**Numeração do ADR dos testes de integração:** o arquivo e o índice são o **0036**
([`adr/README.md`](../adr/README.md)), mas o texto ainda diz 0035 em:
- links quebrados (o `check-docs` acusa): `architecture.md` (2), `development.md` (1),
  `roadmap.md` (2);
- o título do próprio ADR (`0036-testes-de-integracao-com-testcontainers.md`, linha 1) e o
  status do ADR 0034 ("em parte substituído por 0035");
- comentários: `backend/pom.xml` (2), `application-test.properties`,
  `TestcontainersConfiguration.java`, `MySqlImageTest.java`, `IntegrationTest.java`.

## 9. Documentação ao concluir

- [`domain-model.md`](../domain-model.md), `Contract`: "Valor e parcelas" (o aditivo escolhe as
  parcelas, R2), "Versionamento (aditivos)" e os endpoints de apoio (a prévia do aditivo, R1).
- [`architecture.md`](../architecture.md): o que a tela trouxer de padrão novo.
- [`roadmap.md`](../roadmap.md): o aditivo concluído; o esboço do `billing` (seção 8) no lugar
  de "`Payment`" e "Saldo do paciente"; o que a feature deixou para depois (seção 2).
- ADRs da seção 10; o ADR 0032 com o status "aceito (em parte substituído por NNNN)".
- Remover esta spec e a entrada em [`specs/README.md`](README.md).

## 10. ADRs a registrar

- **Parcelas escolhidas no aditivo** (R2), substituindo em parte o ADR 0032. *Descartadas:*
  copiar sempre; só na prorrogação; descontar as já pagas.
- **Prévia do aditivo num endpoint próprio** (R1), se a parte 2 confirmar o desenho.
  *Descartadas:* a prévia da criação; o cálculo no front.
