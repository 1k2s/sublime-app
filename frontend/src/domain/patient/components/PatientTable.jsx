import { Pencil } from "lucide-react";
import { Badge } from "@/shared/components/Badge";
import { DataTable } from "@/shared/components/DataTable";
import { IconButton } from "@/shared/components/IconButton";
import { ListMessage } from "@/shared/components/ListMessage";
import { maskCpf, maskPhone } from "@/shared/utils/masks";

const MESSAGES = {
  loading: "Carregando pacientes...",
  error: "Não foi possível carregar os pacientes",
  filtered: "Nenhum paciente encontrado",
  empty: "Nenhum paciente cadastrado",
  emptyHint: "Clique em Novo paciente para começar.",
};

export function PatientTable({
  patients = [],
  isLoading = false,
  isError = false,
  hasRecords = false,
  selectedPatientId = null,
  onViewDetails,
}) {
  if (isLoading || isError || patients.length === 0) {
    return <ListMessage isLoading={isLoading} isError={isError} hasRecords={hasRecords} messages={MESSAGES} />;
  }

  const columns = [
    { key: "name", header: "Nome", cell: (patient) => patient.name },
    { key: "cpf", header: "CPF", cell: (patient) => (patient.cpf ? maskCpf(patient.cpf) : "--") },
    { key: "phone", header: "Telefone", cell: (patient) => (patient.phone ? maskPhone(patient.phone) : "--") },
    { key: "city", header: "Cidade", cell: (patient) => patient.address?.city || "--" },
    {
      key: "status",
      header: "Status",
      cell: (patient) => (
        <Badge variant={patient.active ? "active" : "inactive"}>{patient.active ? "Ativo" : "Inativo"}</Badge>
      ),
    },
    {
      key: "actions",
      header: "Ações",
      align: "center",
      cell: (patient) => (
        <IconButton label={`Ver ou editar ${patient.name}`} title="Ver / Editar" onClick={() => onViewDetails?.(patient)}>
          <Pencil size={16} />
        </IconButton>
      ),
    },
  ];

  // A linha inteira abre o detalhe; a linha aberta fica marcada
  return (
    <DataTable
      columns={columns}
      rows={patients}
      onRowClick={(patient) => onViewDetails?.(patient)}
      selectedRowKey={selectedPatientId}
    />
  );
}
