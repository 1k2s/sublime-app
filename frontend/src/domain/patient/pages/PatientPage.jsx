/**
 * ======================================================================================
 * PÁGINA PRINCIPAL: PatientPage
 * ======================================================================================
 *
 * Ponto de entrada do módulo de pacientes acessível pela rota `/patients`:
 * - Gerencia o estado de visualização através de uma máquina de estados simples:
 *   - "list": exibe cabeçalho, filtros de busca/status, tabela de pacientes e modal de detalhes.
 *   - "create": exibe o fluxo de cadastro em 3 etapas (apenas para Admin).
 *   - "edit": exibe o fluxo de edição com campos pré-carregados (apenas para Admin).
 *
 * 🛡️ CONTROLE DE ACESSO POR PERFIL (RBAC):
 * - `admin`: Acesso total (visualização, busca, criação e edição).
 * - `provider`: Acesso restrito somente leitura à listagem e detalhes do paciente.
 *   O botão "+ Novo paciente" fica oculto e qualquer tentativa direta de abrir o formulário
 *   é redirecionada para a listagem.
 */

import { useState, useTransition } from "react";
import { Plus } from "lucide-react";
import { useAuth } from "@/domain/auth/context/useAuth";
import { ROLES } from "@/domain/auth/roles";
import { PageHeader } from "@/shared/ui/PageHeader";
import { Button } from "@/shared/ui/Button";
import { usePatients, useUpdatePatient } from "../api/usePatients";
import { PatientFilters } from "../components/PatientFilters";
import { PatientTable } from "../components/PatientTable";
import { PatientDetailsModal } from "../components/PatientDetailsModal";
import { PatientWizard } from "../components/wizard/PatientWizard";
import styles from "./PatientPage.module.css";

export function PatientPage() {
  // Obtém o usuário autenticado na sessão atual
  const { user } = useAuth();
  const isAdmin = user?.role === ROLES.ADMIN.value;

  // Estados locais da página
  const [view, setView] = useState("list"); // Modos: "list" | "create" | "edit"
  const [search, setSearch] = useState(""); // Termo de busca digitado
  const [status, setStatus] = useState("ACTIVE"); // Filtro de status inicial ("ACTIVE" conforme design)
  const [selectedPatient, setSelectedPatient] = useState(null); // Paciente selecionado para o modal de detalhes
  const [editingPatient, setEditingPatient] = useState(null); // Paciente selecionado para edição no wizard
  const [, startTransition] = useTransition();

  // Consulta reativa de pacientes via TanStack Query
  const { data: patients = [], isLoading } = usePatients({ search, status });
  const updatePatientMutation = useUpdatePatient();

  // Regra de segurança: apenas admin pode estar no modo de formulário (criação ou edição)
  const isFormMode = (view === "create" || view === "edit") && isAdmin;
  const isEditing = view === "edit";

  /**
   * Alterna o status do paciente entre Ativo e Inativo
   */
  const handleToggleStatus = async (patient, nextStatus) => {
    try {
      await updatePatientMutation.mutateAsync({
        id: patient.id,
        data: { ...patient, status: nextStatus },
      });
      // Atualiza o estado local do modal se estiver aberto
      setSelectedPatient((prev) => (prev ? { ...prev, status: nextStatus } : null));
    } catch (err) {
      console.error("Erro ao alterar status do paciente:", err);
    }
  };

  /**
   * Atualiza o termo de busca utilizando useTransition para manter a digitação fluida
   */
  const handleSearchChange = (val) => {
    startTransition(() => {
      setSearch(val);
    });
  };

  /**
   * Atualiza a aba de status selecionada (Todos, Ativos, Inativos)
   */
  const handleStatusChange = (val) => {
    setStatus(val);
  };

  /**
   * Inicia o fluxo de cadastro de um novo paciente
   */
  const handleStartCreate = () => {
    setEditingPatient(null);
    setView("create");
  };

  /**
   * Inicia o fluxo de edição de um paciente selecionado no modal
   */
  const handleStartEdit = (patient) => {
    setSelectedPatient(null);
    setEditingPatient(patient);
    setView("edit");
  };

  /**
   * Retorna para a listagem resetando o paciente em edição
   */
  const handleBackToList = () => {
    setEditingPatient(null);
    setView("list");
  };

  return (
    <div className={styles.pageContainer}>
      {!isFormMode ? (
        /* MODO 1: TELA DE LISTAGEM (Imagem 1) */
        <>
          {/* Cabeçalho da listagem com botão de novo paciente visível apenas para admin */}
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

          {/* Barra de filtros de busca e pills de status */}
          <PatientFilters
            search={search}
            onSearchChange={handleSearchChange}
            status={status}
            onStatusChange={handleStatusChange}
          />

          {/* Tabela de pacientes com badge e botão de ação */}
          <PatientTable
            patients={patients}
            isLoading={isLoading}
            isAdmin={isAdmin}
            onViewDetails={(patient) => setSelectedPatient(patient)}
          />

          {/* Modal de detalhes aberto ao clicar em "Ver / Editar" ou "Ver" */}
          <PatientDetailsModal
            patient={selectedPatient}
            isAdmin={isAdmin}
            onClose={() => setSelectedPatient(null)}
            onEdit={handleStartEdit}
            onToggleStatus={handleToggleStatus}
          />
        </>
      ) : (
        /* MODO 2: WIZARD DE CRIAÇÃO OU EDIÇÃO (Imagem 2) */
        <>
          {/* Cabeçalho com botão de retorno e títulos contextuais */}
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

          {/* Orquestrador do formulário em 3 etapas com prévia lateral */}
          <PatientWizard
            initialData={editingPatient}
            onCancel={handleBackToList}
            onSuccess={handleBackToList}
          />
        </>
      )}
    </div>
  );
}
