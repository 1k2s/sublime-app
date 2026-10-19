// Lançamento do prestador: vai adicionando pacientes ao carrinho só com o Enter e grava tudo de uma vez
import { useState } from "react";
import { PageHeader } from "@/shared/ui/PageHeader";
import { toDateInputValue } from "@/shared/utils/date";
import { useConsultations, useCreateConsultations, usePreviewConsultation } from "../api/useConsultations";
import { ConsultationCart } from "../components/ConsultationCart";
import { ConsultationForm } from "../components/ConsultationForm";
import { ConsultationTotal } from "../components/ConsultationTotal";
import { useConsultationCart } from "../hooks/useConsultationCart";
import styles from "./ConsultationPage.module.css";

export function ConsultationPage() {
  const [today] = useState(() => toDateInputValue(new Date()));
  const cart = useConsultationCart();
  const preview = usePreviewConsultation();
  const createConsultations = useCreateConsultations();
  const todayConsultations = useConsultations({ date: today });

  function handleSelectPatient(patient) {
    preview.reset();
    cart.selectPatient(patient);
  }

  // Busca os valores do contrato antes de pôr no carrinho: paciente sem contrato não entra
  function handleAdd() {
    const payload = cart.validateDraft();
    if (!payload) return;

    createConsultations.reset();
    const { patient, technique } = cart;
    preview.mutate(payload, { onSuccess: (result) => cart.addItem({ patient, technique, ...result }) });
  }

  function handleFinish() {
    const payloads = cart.items.map((item) => ({
      patientId: item.patient.id,
      techniqueId: item.technique.id,
      arrivalTime: item.arrivalTime,
    }));
    createConsultations.mutate(payloads, { onSuccess: cart.clear });
  }

  return (
    <div className={styles.page}>
      <PageHeader title="Lançamentos" description="Registre a chegada dos pacientes atendidos." />

      <div className={styles.grid}>
        <ConsultationForm
          className={styles.block}
          patient={cart.patient}
          technique={cart.technique}
          arrivalTime={cart.arrivalTime}
          errors={cart.errors}
          onSelectPatient={handleSelectPatient}
          onSelectTechnique={cart.selectTechnique}
          onArrivalTimeChange={cart.setArrivalTime}
          onAdd={handleAdd}
          isAdding={preview.isPending}
          addError={preview.error}
        />

        <ConsultationCart
          className={styles.block}
          items={cart.items}
          onRemove={cart.removeItem}
          onFinish={handleFinish}
          isFinishing={createConsultations.isPending}
          finishError={createConsultations.error}
          finishedCount={createConsultations.data?.length}
        />

        <ConsultationTotal
          className={styles.total}
          consultations={todayConsultations.data}
          isLoading={todayConsultations.isLoading}
        />
      </div>
    </div>
  );
}
