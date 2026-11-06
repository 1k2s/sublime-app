// Página de gerenciamento de pacientes (/patients)
import { useState, useTransition } from "react";
import { Plus } from "lucide-react";
import { useAuth } from "@/domain/auth/context/useAuth";
import { ROLES } from "@/domain/auth/roles";
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
import { Button } from "@/shared/ui/Button";
import { PageHeader } from "@/shared/ui/PageHeader";
import { showError } from "@/shared/utils/alert";
import styles from "@/domain/patient/pages/PatientPage.module.css";

export function PatientPage() {
  const { user } = useAuth();
  const isAdmin = user?.role === ROLES.ADMIN.value;

  const [view, setView] = useState("list");
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState("ACTIVE");
  const [selectedPatient, setSelectedPatient] = useState(null);
  const [editingPatient, setEditingPatient] = useState(null);
  const [, startTransition] = useTransition();

  const { data: patients = [], isLoading } = usePatients({ search, status });
  const createPatientMutation = useCreatePatient();
  const updatePatientMutation = useUpdatePatient();
  const deactivatePatientMutation = useDeactivatePatient();
  const activatePatientMutation = useActivatePatient();

  const isFormMode = (view === "create" || view === "edit") && isAdmin;
  const isEditing = view === "edit";
  const isSubmitting = createPatientMutation.isPending || updatePatientMutation.isPending;
  const isTogglingStatus =
    deactivatePatientMutation.isPending || activatePatientMutation.isPending;

  // Mantém o modal sincronizado caso a lista de pacientes seja atualizada
  const currentSelectedPatient = selectedPatient
    ? patients.find((p) => p.id === selectedPatient.id) ?? selectedPatient
    : null;

  function handleToggleStatus(patient) {
    if (patient.active) {
      deactivatePatientMutation.mutate(patient.id, {
        onSuccess: () =>
          setSelectedPatient((prev) => (prev ? { ...prev, active: false } : null)),
        onError: (error) => showError(error.message),
      });
    } else {
      activatePatientMutation.mutate(patient.id, {
        onSuccess: () =>
          setSelectedPatient((prev) => (prev ? { ...prev, active: true } : null)),
        onError: (error) => showError(error.message),
      });
    }
  }

  function handleSavePatient(payload, { applyApiErrors }) {
    if (isEditing && editingPatient) {
      updatePatientMutation.mutate(
        { id: editingPatient.id, data: payload },
        {
          onSuccess: handleBackToList,
          onError: (error) => {
            applyApiErrors?.(error.response?.data?.errors);
            showError(error.message);
          },
        }
      );
    } else {
      createPatientMutation.mutate(payload, {
        onSuccess: handleBackToList,
        onError: (error) => {
          applyApiErrors?.(error.response?.data?.errors);
          showError(error.message);
        },
      });
    }
  }

  function handleSearchChange(val) {
    startTransition(() => {
      setSearch(val);
    });
  }

  function handleStatusChange(val) {
    setStatus(val);
  }

  function handleStartCreate() {
    setEditingPatient(null);
    setView("create");
  }

  function handleStartEdit(patient) {
    setSelectedPatient(null);
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
              isAdmin ? (
                <Button onClick={handleStartCreate}>
                  <Plus size={18} /> Novo paciente
                </Button>
              ) : null
            }
          />

          <PatientFilters
            search={search}
            onSearchChange={handleSearchChange}
            status={status}
            onStatusChange={handleStatusChange}
          />

          <PatientTable
            patients={patients}
            isLoading={isLoading}
            isAdmin={isAdmin}
            onViewDetails={(patient) => setSelectedPatient(patient)}
          />

          <PatientDetailsModal
            patient={currentSelectedPatient}
            isAdmin={isAdmin}
            isTogglingStatus={isTogglingStatus}
            onClose={() => setSelectedPatient(null)}
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
