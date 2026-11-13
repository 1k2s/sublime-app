// Lançamento: a barra fixa em cima (um atendimento por vez, gravado na hora) e os lançamentos de
// hoje embaixo, com rolagem própria. O "hoje", o total e o que a liberação permite vêm do resumo
// do dia (backend). O PROVIDER lança por ele mesmo; o ADMIN escolhe o prestador
import { useState } from "react";
import { useAuth } from "@/domain/auth/context/useAuth";
import { ROLES } from "@/domain/auth/roles";
import { useTechniques } from "@/domain/pricing/api/usePricing";
import { useProviders } from "@/domain/provider/api/useProviders";
import { PageHeader } from "@/shared/components/PageHeader";
import { showError, showSuccess } from "@/shared/utils/alert";
import { formatDate } from "@/shared/utils/date";
import {
  useCreateConsultation,
  useDayConsultations,
  useDaySummary,
  useDeactivateConsultation,
  useLaunchPatients,
  useRecentPatients,
} from "../api/useConsultations";
import { DaySummary } from "../components/DaySummary";
import { LaunchBar } from "../components/LaunchBar";
import { RecentPatients } from "../components/RecentPatients";
import { TodayConsultations } from "../components/TodayConsultations";
import { useLaunchDraft } from "../hooks/useLaunchDraft";
import { useLaunchProvider } from "../hooks/useLaunchProvider";
import styles from "./ConsultationPage.module.css";

export function ConsultationPage() {
  const { user } = useAuth();
  const isAdmin = user?.role === ROLES.ADMIN.value;
  const [providerId, setProviderId] = useLaunchProvider(isAdmin);
  // O ADMIN informa o prestador nas leituras; sem ele escolhido, elas esperam
  const readingProviderId = isAdmin ? providerId || undefined : undefined;
  const canRead = !isAdmin || Boolean(providerId);

  const summaryQuery = useDaySummary(readingProviderId, canRead);
  const summary = summaryQuery.data;
  const today = summary?.date;
  const retroactiveRemaining = summary?.retroactiveLaunchesRemaining ?? 0;

  const recentQuery = useRecentPatients(readingProviderId, canRead);
  const dayQuery = useDayConsultations(readingProviderId, today, canRead);
  const patientsQuery = useLaunchPatients();
  const techniquesQuery = useTechniques();
  const providersQuery = useProviders({ enabled: isAdmin });

  const draft = useLaunchDraft({
    isAdmin,
    providerId,
    today,
    canLaunchOnOtherDay: isAdmin || retroactiveRemaining > 0,
  });
  const createConsultation = useCreateConsultation();
  const deactivateConsultation = useDeactivateConsultation();
  // O último lançamento desta tela: destacado na lista, com o aviso de saldo da resposta
  const [lastLaunched, setLastLaunched] = useState(null);

  const providerOptions = (providersQuery.data ?? [])
    .filter((provider) => provider.active)
    .map((provider) => ({ value: String(provider.id), label: provider.name }));
  const patientOptions = (patientsQuery.data ?? []).map((patient) => ({
    value: String(patient.id),
    label: patient.name,
    description: patient.birthDate ? `nasc. ${formatDate(patient.birthDate)}` : undefined,
  }));
  const techniqueOptions = (techniquesQuery.data ?? [])
    .filter((technique) => technique.active)
    .map((technique) => ({ value: String(technique.id), label: technique.name }));
  const patientName = patientsQuery.data?.find((patient) => String(patient.id) === draft.fields.patientId)?.name;
  const todayConsultations = (dayQuery.data ?? []).filter((consultation) => consultation.active);

  function handleProviderChange(value) {
    setProviderId(value);
    setLastLaunched(null);
    draft.reset();
  }

  // Lançado: o aviso de sucesso (o Enter fecha) e, ao fechar, a barra limpa com o foco no Paciente
  // (depois do aviso: senão ele devolveria o foco ao botão Lançar)
  function handleLaunch() {
    if (createConsultation.isPending) return;
    const request = draft.buildRequest();
    if (!request) return;
    createConsultation.mutate(request, {
      onSuccess: (created) => {
        setLastLaunched({ id: created.id, remainingSessions: created.remainingSessions });
        showSuccess("Atendimento lançado com sucesso!").then(() => draft.reset());
      },
      onError: (error) => {
        draft.applyApiErrors(error.response?.data?.errors);
        showError(error.message);
      },
    });
  }

  function handleUndo(id) {
    deactivateConsultation.mutate(id, { onError: (error) => showError(error.message) });
  }

  return (
    <div className={styles.page}>
      <PageHeader
        title="Lançamentos"
        description="Paciente, técnica e Enter. O resto já vem preenchido."
        action={
          canRead ? (
            <DaySummary summary={summary} isLoading={summaryQuery.isLoading} />
          ) : (
            <p className={styles.hint}>Escolha o prestador para lançar.</p>
          )
        }
      />

      <LaunchBar
        draft={draft}
        isAdmin={isAdmin}
        providerId={providerId}
        onProviderChange={handleProviderChange}
        today={today}
        providerOptions={providerOptions}
        patientOptions={patientOptions}
        techniqueOptions={techniqueOptions}
        retroactiveRemaining={retroactiveRemaining}
        patientName={patientName}
        onLaunch={handleLaunch}
        isLaunching={createConsultation.isPending}
      >
        <RecentPatients
          recents={recentQuery.data}
          selectedPatientId={draft.fields.patientId}
          onPick={draft.applyRecent}
        />
      </LaunchBar>

      {canRead && (
        <TodayConsultations
          className={styles.list}
          consultations={todayConsultations}
          isLoading={summaryQuery.isLoading || dayQuery.isLoading}
          isError={summaryQuery.isError || dayQuery.isError}
          lastLaunched={lastLaunched}
          onUndo={handleUndo}
          undoingId={deactivateConsultation.isPending ? deactivateConsultation.variables : null}
        />
      )}
    </div>
  );
}
