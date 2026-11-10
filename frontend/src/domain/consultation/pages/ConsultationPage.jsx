// Lançamento do prestador: vai adicionando pacientes ao carrinho só com o Enter e grava tudo de uma vez.
// A API lança um atendimento por requisição: o "Finalizar" faz um POST por item (ver useCreateConsultations)
import { useRef, useState } from "react";
import { useAuth } from "@/domain/auth/context/useAuth";
import { useCurrentProvider } from "@/domain/provider/api/useProviders";
import { Card } from "@/shared/ui/Card";
import { PageHeader } from "@/shared/ui/PageHeader";
import { showError, showWarning } from "@/shared/utils/alert";
import { toDateInputValue } from "@/shared/utils/date";
import { useConsultations, useCreateConsultations, useEligibleContracts } from "../api/useConsultations";
import { ConsultationCart } from "../components/ConsultationCart";
import { ConsultationForm } from "../components/ConsultationForm";
import { ConsultationTotal } from "../components/ConsultationTotal";
import { ContractChoiceModal } from "../components/ContractChoiceModal";
import styles from "./ConsultationPage.module.css";

export function ConsultationPage() {
  const { user } = useAuth();
  const currentProvider = useCurrentProvider(user?.id);
  const providerId = currentProvider.data?.id;

  const [today] = useState(() => toDateInputValue(new Date()));
  // Itens do carrinho, ainda não gravados: o que o formulário entregou + { key, contract, error? }
  const [items, setItems] = useState([]);
  const nextKey = useRef(1);
  // Item à espera da escolha de contrato (paciente com dois vinculados na data)
  const [pendingChoice, setPendingChoice] = useState(null);
  const eligibleContracts = useEligibleContracts();
  const createConsultations = useCreateConsultations();
  const todayConsultations = useConsultations({ providerId, from: today, to: today });

  // contract: o vinculado escolhido (como vem do eligible-contracts) ou null no Avulso
  function addItem(draft, contract) {
    setItems((current) => [...current, { ...draft, contract, key: nextKey.current++ }]);
  }

  // Antes de pôr no carrinho, descobre os contratos vinculados: nenhum = Avulso, um = esse, dois = o prestador escolhe
  function handleAdd(draft, clearDraft) {
    createConsultations.reset();
    const params = { patientId: draft.patient.id, techniqueId: draft.technique.id, occurredAt: draft.occurredAt };
    eligibleContracts.mutate(params, {
      onSuccess: (contracts) => {
        if (contracts.length > 1) {
          setPendingChoice({ draft, contracts, clearDraft });
          return;
        }
        addItem(draft, contracts[0] ?? null);
        clearDraft();
      },
      onError: (error) => showError(error.message),
    });
  }

  function handleChooseContract(contract) {
    addItem(pendingChoice.draft, contract);
    pendingChoice.clearDraft();
    setPendingChoice(null);
  }

  // Monta o payload da API por item. Os gravados saem do carrinho; os que falharam ficam com a mensagem da API
  function handleFinish() {
    const requests = items.map((item) => ({
      key: item.key,
      payload: {
        patientId: item.patient.id,
        techniqueId: item.technique.id,
        contractId: item.contract?.contractId ?? null,
        providerId,
        occurredAt: item.occurredAt,
        durationMinutes: item.durationMinutes,
        status: item.status,
      },
    }));
    createConsultations.mutate(requests, {
      onSuccess: (results) => {
        const resultByKey = new Map(results.map((result) => [result.key, result]));
        setItems((current) =>
          current
            .filter((item) => !resultByKey.get(item.key)?.created)
            .map((item) => ({ ...item, error: resultByKey.get(item.key)?.error })),
        );
        notifyResults(results);
      },
      onError: (error) => showError(error.message),
    });
  }

  if (currentProvider.isLoading) {
    return <PageHeader title="Lançamentos" description="Carregando..." />;
  }

  if (!currentProvider.data) {
    return (
      <div className={styles["page-wrapper"]}>
        <PageHeader title="Lançamentos" description="Registre os pacientes atendidos." />
        <Card title="Lançamento indisponível">
          {currentProvider.isError
            ? currentProvider.error.message
            : "Seu usuário não está vinculado a um prestador. Só prestadores lançam atendimentos."}
        </Card>
      </div>
    );
  }

  return (
    <div className={styles["page-wrapper"]}>
      <PageHeader title="Lançamentos" description="Registre os pacientes atendidos." />

      <div className={styles["page-grid"]}>
        <ConsultationForm
          className={styles["page-block"]}
          onAdd={handleAdd}
          isAdding={eligibleContracts.isPending}
          isLocked={createConsultations.isPending}
        />

        <ConsultationCart
          className={styles["page-block"]}
          items={items}
          onRemove={(key) => setItems((current) => current.filter((item) => item.key !== key))}
          onFinish={handleFinish}
          isFinishing={createConsultations.isPending}
          finishedCount={createConsultations.data?.filter((result) => result.created).length}
        />

        <ConsultationTotal
          className={styles["page-total"]}
          consultations={todayConsultations.data}
          isLoading={todayConsultations.isLoading}
        />
      </div>

      {pendingChoice && (
        <ContractChoiceModal
          patientName={pendingChoice.draft.patient.name}
          contracts={pendingChoice.contracts}
          onChoose={handleChooseContract}
          onClose={() => setPendingChoice(null)}
        />
      )}
    </div>
  );
}

// Um alerta só: erro se algum item falhou (ele ficou no carrinho com o motivo), aviso se só esgotou saldo.
// Saldo esgotado não é erro: a API aceitou o lançamento (ver domain-model.md, Consultation, "Saldo de sessões")
function notifyResults(results) {
  const failedCount = results.filter((result) => result.error).length;
  const exhaustedNames = results
    .filter((result) => result.created?.remainingSessions < 0)
    .map((result) => result.created.patientName);

  const messages = [];
  if (failedCount > 0) {
    messages.push(`${failedCount} lançamento(s) não gravado(s). O motivo está no item do carrinho.`);
  }
  if (exhaustedNames.length > 0) {
    messages.push(`Saldo de sessões esgotado no contrato de: ${exhaustedNames.join(", ")} (lançamento gravado).`);
  }

  if (failedCount > 0) showError(messages.join(" "));
  else if (messages.length > 0) showWarning(messages.join(" "));
}
