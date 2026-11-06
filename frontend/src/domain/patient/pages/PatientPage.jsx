import { useState, useTransition } from "react";
import { Plus } from "lucide-react";
import { useAuth } from "@/domain/auth/context/useAuth";
import { ROLES } from "@/domain/auth/roles";
import { PageHeader } from "@/shared/ui/PageHeader";
import { Button } from "@/shared/ui/Button";
import { showError } from "@/shared/utils/alert";
import {
  usePatients,
  useCreatePatient,
  useUpdatePatient,
  useDeactivatePatient,
  useActivatePatient,
} from "../api/usePatients";
import { toPatientRequest } from "../patientForm";
import { PatientFilters } from "../components/PatientFilters";
import { PatientTable } from "../components/PatientTable";
import { PatientDetailsModal } from "../components/PatientDetailsModal";
import { PatientWizard } from "../components/wizard/PatientWizard";
import styles from "./PatientPage.module.css";

export function PatientPage() {
  const { user } = useAuth();
  const isAdmin = user?.role === ROLES.ADMIN.value;

  const [view, setView] = useState("list"); // "list" | "create" | "edit"
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

  // Inativar = DELETE; reativar = PATCH /activate. A API não devolve o paciente (204),
  // então o modal aberto é atualizado aqui; a lista volta pela invalidação
  const handleToggleStatus = (patient) => {
    const mutation = patient.active ? deactivatePatientMutation : activatePatientMutation;
    mutation.mutate(patient.id, {
      onSuccess: () =>
        setSelectedPatient((prev) => (prev ? { ...prev, active: !patient.active } : null)),
      onError: (error) => showError(error.message),
    });
  };

  const handleSearchChange = (value) => {
    startTransition(() => setSearch(value));
  };

  const handleStartCreate = () => {
    setEditingPatient(null);
    setView("create");
  };

  const handleStartEdit = (patient) => {
    setSelectedPatient(null);
    setEditingPatient(patient);
    setView("edit");
  };

  const handleBackToList = () => {
    setEditingPatient(null);
    setView("list");
  };

  // Na edição, reenvia a origem atual: nula no PUT tornaria o paciente "da clínica"
  const handleSave = (formValues) => {
    const callbacks = {
      onSuccess: handleBackToList,
      onError: (error) => showError(error.message),
    };

    if (isEditing) {
      const data = toPatientRequest(formValues, editingPatient.referringProviderId);
      updatePatientMutation.mutate({ id: editingPatient.id, data }, callbacks);
    } else {
      createPatientMutation.mutate(toPatientRequest(formValues), callbacks);
    }
  };

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
            onStatusChange={setStatus}
          />

          <PatientTable
            patients={patients}
            isLoading={isLoading}
            isAdmin={isAdmin}
            onViewDetails={setSelectedPatient}
          />

          <PatientDetailsModal
            patient={selectedPatient}
            isAdmin={isAdmin}
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
            onSubmit={handleSave}
            onCancel={handleBackToList}
          />
        </>
      )}
    </div>
  );
}
