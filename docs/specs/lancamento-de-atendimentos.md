# Spec: tela de lançamento de atendimentos ligada à API real

## Status

- **Backend:** `/api/consultations` pronto (lançar, listar, contratos vinculados, saldo,
  buscar, inativar). Nada muda no backend nesta feature.
- **Frontend:** implementado (só o front), sem teste pela tela ainda. A tela `/consultations`
  usava um mock com endpoints que não existem (`/consultations/preview` e
  `/consultations/batch`); agora chama a API real.
- Branch: a criar (`feature/frontend-consultation`).

## Objetivo

O prestador monta um carrinho com os atendimentos cobrados (paciente, técnica, data,
duração e status) e grava tudo com "Finalizar". Quando o paciente tem dois contratos
vinculados na data, escolhe de qual sai a sessão. O total de repasse do dia vem da API.

Depende de: `auth` (usuário logado), `provider` (prestador do usuário), `patient` (busca de
paciente), `pricing` (busca de técnica e durações da tabela vigente).

## Regras

1. **Carrinho com um POST por item.** O "Finalizar" chama `POST /api/consultations` uma
   vez por item, em sequência. Os gravados saem do carrinho; os que falharam ficam com a
   mensagem da API. O loading do botão cobre o laço inteiro e trava o carrinho e o
   "Adicionar" até o último POST.
   - *Por quê:* a API grava um atendimento por requisição. É a solução por ora.
   - *Consequência aceita:* falha parcial (parte do carrinho gravada); o carrinho não
     mostra repasse, porque ele só existe depois do lançamento.
   - *Pendente:* lançamento em lote no backend (tudo ou nada), ver `roadmap.md`.
2. **Só o `PROVIDER` lança, e lança os próprios atendimentos.** O `providerId` é do
   prestador vinculado ao usuário logado, achado pelo `GET /api/providers` (filtro pelo
   `userId`). Usuário sem prestador (ex: `ADMIN`) vê um aviso no lugar do formulário.
   - *Pendente:* `providerId` no login, para não baixar a lista inteira.
   - A permissão é do backend (princípio 3 do front), ainda não aplicada.
3. **Data, não horário:** `occurredAt` (hoje por padrão, não futura).
4. **Duração e status obrigatórios.** Duração: grupo por duração → opções da tabela
   vigente da técnica; grupo por frequência (Pilates em Grupo) → campo livre. Status:
   `ATTENDED` (padrão), `NO_SHOW`, `CANCELED_LATE`.
5. **Contrato definido ao adicionar** (`GET /api/consultations/eligible-contracts`): vazio →
   Avulso; um → esse; dois → modal de escolha (titular ou beneficiário, plano, validade,
   se debita sessão ou vai pelo plano equivalente, saldo). O front só segue a resposta.
6. **Saldo esgotado é aviso:** `remainingSessions < 0` na resposta do POST → aviso depois
   de gravar.
7. **Total do dia:** soma do `payoutValue` dos lançamentos **ativos** de hoje do prestador
   (`GET /api/consultations?providerId&from&to`).

## Desenho (implementado)

- `provider/api/useProviders.js`: `useCurrentProvider(userId)`.
- `consultation/api/consultationApi.js`: `listConsultations`, `listEligibleContracts`,
  `createConsultation`. `useConsultations.js`: `useConsultations`, `useEligibleContracts`,
  `useCreateConsultations` (laço sequencial numa única `mutationFn`).
- `consultation/consultationStatus.js`: os três status com rótulo.
- `hooks/useConsultationCart.js`: item em edição, `addItem(payload, contract)`,
  `applyResults(results)`.
- Componentes: `ConsultationForm` (data, duração, status), `ContractChoiceModal` (novo),
  `ConsultationCart` (contrato no lugar do valor, erro por item), `ConsultationAgenda`
  (duração), `ConsultationTotal` (`payoutValue` dos ativos).
- `shared/utils/alert.js`: `showWarning`.

## Tarefas

- [x] Front: camada `api/` e hooks com os endpoints reais.
- [x] Front: prestador logado pelo `GET /providers`.
- [x] Front: schema, formulário (data, duração, status), modal de contrato.
- [x] Front: carrinho com um POST por item, loading até o fim, erro por item.
- [x] Front: total pelo `payoutValue`; mocks removidos; `npm run build` e `npm run lint`.
- [ ] Testar pela tela com o backend rodando: Avulso, um contrato, dois contratos,
      saldo negativo, Pilates em Grupo sem contrato (400 no item), loading, total.
- [ ] Ao concluir: regras para o `architecture.md`/`roadmap.md`, remover esta spec.

## ADRs a registrar

- Nenhum agora. Se o lote no backend for feito, a decisão vira ADR.
