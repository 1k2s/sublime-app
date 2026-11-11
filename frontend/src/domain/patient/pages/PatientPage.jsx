// Página de gerenciamento de pacientes (/patients)
import { useState } from "react";
import { Plus } from "lucide-react";
import {
  useActivatePatient,
  useCreatePatient,
  useDeactivatePatient,
  usePatients,
  useUpdatePatient,
} from "@/domain/patient/api/usePatients";
import { PatientDetailsModal } from "@/domain/patient/components/PatientDetailsModal";
import { PatientFilters } from "@/domain/patient/components/PatientFilters";
import { PatientTable } from "@/domain/patient/components/PatientTable";
import { PatientWizard } from "@/domain/patient/components/wizard/PatientWizard";
import { filterPatients } from "@/domain/patient/filterPatients";
import { Button } from "@/shared/components/Button";
import { PageHeader } from "@/shared/components/PageHeader";
import { showError } from "@/shared/utils/alert";
import styles from "@/domain/patient/pages/PatientPage.module.css";

export function PatientPage() {
  const [view, setView] = useState("list");
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState("ACTIVE");
  const [selectedPatientId, setSelectedPatientId] = useState(null);
  const [editingPatient, setEditingPatient] = useState(null);

  const { data: patients = [], isLoading, isError } = usePatients();
  const createPatientMutation = useCreatePatient();
  const updatePatientMutation = useUpdatePatient();
  const deactivatePatientMutation = useDeactivatePatient();
  const activatePatientMutation = useActivatePatient();

  const visiblePatients = filterPatients(patients, { search, status });
  // Lido da lista inteira: depois de inativar ou reativar, o modal mostra o estado novo
  const selectedPatient = patients.find((patient) => patient.id === selectedPatientId) ?? null;
  const isFormMode = view === "create" || view === "edit";
  const isEditing = view === "edit";
  const isSubmitting = createPatientMutation.isPending || updatePatientMutation.isPending;
  const isTogglingStatus =
    deactivatePatientMutation.isPending || activatePatientMutation.isPending;

  function handleToggleStatus(patient) {
    const mutation = patient.active ? deactivatePatientMutation : activatePatientMutation;
    mutation.mutate(patient.id, { onError: (error) => showError(error.message) });
  }

  function handleSavePatient(payload, { applyApiErrors }) {
    const options = {
      onSuccess: handleBackToList,
      onError: (error) => {
        applyApiErrors?.(error);
        showError(error.message);
      },
    };

    if (isEditing && editingPatient) {
      updatePatientMutation.mutate({ id: editingPatient.id, data: payload }, options);
    } else {
      createPatientMutation.mutate(payload, options);
    }
  }

  function handleStartCreate() {
    setEditingPatient(null);
    setView("create");
  }

  function handleStartEdit(patient) {
    setSelectedPatientId(null);
    setEditingPatient(patient);
    setView("edit");
  }

  function handleBackToList() {
    setEditingPatient(null);
    setView("list");
  }

  return (
    <div className={styles.pageContainer}>
      {!isFormMode ? (
        <>
          <PageHeader
            className={styles.pageHeader}
            title="Pacientes"
            description="Gerencie o cadastro de pacientes da clínica."
            action={
              <Button onClick={handleStartCreate}>
                <Plus size={18} /> Novo paciente
              </Button>
            }
          />

          <PatientFilters
            search={search}
            onSearchChange={setSearch}
            status={status}
            onStatusChange={setStatus}
          />

          <PatientTable
            patients={visiblePatients}
            isLoading={isLoading}
            isError={isError}
            hasRecords={patients.length > 0}
            onViewDetails={(patient) => setSelectedPatientId(patient.id)}
          />

          <PatientDetailsModal
            patient={selectedPatient}
            isTogglingStatus={isTogglingStatus}
            onClose={() => setSelectedPatientId(null)}
            onEdit={handleStartEdit}
            onToggleStatus={handleToggleStatus}
          />
        </>
      ) : (
        <>
          <PageHeader
            className={styles.pageHeader}
            title={isEditing ? "Editar paciente" : "Novo paciente"}
            description={
              isEditing
                ? "Atualize as informações do paciente cadastrado."
                : "Cadastre um novo paciente de forma simples e rápida."
            }
            onBack={handleBackToList}
          />

          <PatientWizard
            initialData={editingPatient}
            isSubmitting={isSubmitting}
            onCancel={handleBackToList}
            onSubmit={handleSavePatient}
          />
        </>
      )}
    </div>
  );
}
