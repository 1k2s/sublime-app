// Lançamento do prestador: vai adicionando pacientes ao carrinho só com o Enter e grava tudo de uma vez
import { PageHeader } from "@/shared/ui/PageHeader";
import { usePreviewConsultation, useCreateConsultations } from "../api/useConsultations";
import { ConsultationCart } from "../components/ConsultationCart";
import { ConsultationForm } from "../components/ConsultationForm";
import { ConsultationTotal } from "../components/ConsultationTotal";
import { useConsultationCart } from "../hooks/useConsultationCart";
import styles from "./ConsultationPage.module.css";

export function ConsultationPage() {
  const cart = useConsultationCart();
  const preview = usePreviewConsultation();
  const createConsultations = useCreateConsultations();

  function handleSelectPatient(patient) {
    preview.reset();
    cart.selectPatient(patient);
  }

  // Busca os valores do contrato antes de pôr no carrinho: paciente sem contrato não entra
  function handleAdd() {
    const payload = cart.validateDraft();
    if (!payload) return;

    createConsultations.reset();
    const patient = cart.patient;
    preview.mutate(payload, { onSuccess: (result) => cart.addItem({ patient, ...result }) });
  }

  function handleFinish() {
    const payloads = cart.items.map((item) => ({ patientId: item.patient.id, arrivalTime: item.arrivalTime }));
    createConsultations.mutate(payloads, { onSuccess: cart.clear });
  }

  return (
    <div className={styles.page}>
      <PageHeader title="Lançamentos" description="Registre a chegada dos pacientes atendidos." />

      <div className={styles.grid}>
        <ConsultationForm
          className={`${styles.block} ${styles.form}`}
          patient={cart.patient}
          arrivalTime={cart.arrivalTime}
          errors={cart.errors}
          onSelectPatient={handleSelectPatient}
          onArrivalTimeChange={cart.setArrivalTime}
          onAdd={handleAdd}
          isAdding={preview.isPending}
          addError={preview.error}
        />

        <ConsultationCart
          className={`${styles.block} ${styles.cart}`}
          items={cart.items}
          onRemove={cart.removeItem}
          onFinish={handleFinish}
          isFinishing={createConsultations.isPending}
          finishError={createConsultations.error}
          finishedCount={createConsultations.data?.length}
        />

        <ConsultationTotal className={`${styles.block} ${styles.total}`} items={cart.items} />
      </div>
    </div>
  );
}
