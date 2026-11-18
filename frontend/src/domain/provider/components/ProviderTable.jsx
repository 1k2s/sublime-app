import { Pencil } from "lucide-react";
import { getRole } from "@/domain/auth/roles";
import { AvatarName } from "@/shared/components/AvatarName";
import { Badge } from "@/shared/components/Badge";
import { DataTable } from "@/shared/components/DataTable";
import { IconButton } from "@/shared/components/IconButton";
import { ListMessage } from "@/shared/components/ListMessage";
import { formatPercentage } from "@/shared/utils/money";

const MESSAGES = {
  loading: "Carregando prestadores...",
  error: "Não foi possível carregar os prestadores",
  filtered: "Nenhum prestador encontrado",
  empty: "Nenhum prestador cadastrado",
  emptyHint: "Clique em Novo prestador para começar.",
};

export function ProviderTable({
  providers = [],
  isLoading = false,
  isError = false,
  hasRecords = false,
  onViewDetails,
}) {
  if (isLoading || isError || providers.length === 0) {
    return <ListMessage isLoading={isLoading} isError={isError} hasRecords={hasRecords} messages={MESSAGES} />;
  }

  const columns = [
    { key: "name", header: "Nome", cell: (provider) => <AvatarName name={provider.name} /> },
    { key: "email", header: "E-mail", cell: (provider) => provider.email || "--" },
    { key: "role", header: "Perfil", cell: (provider) => getRole(provider.role)?.label ?? "--" },
    {
      key: "commission",
      header: "Comissão padrão",
      cell: (provider) => formatPercentage(provider.commissionPercentage),
    },
    {
      key: "referralCommission",
      header: "Comissão de pacientes que trouxe",
      cell: (provider) => formatPercentage(provider.referralCommissionPercentage),
    },
    {
      key: "status",
      header: "Status",
      cell: (provider) => (
        <Badge variant={provider.active ? "active" : "inactive"}>{provider.active ? "Ativo" : "Inativo"}</Badge>
      ),
    },
    {
      key: "actions",
      header: "Ações",
      align: "center",
      cell: (provider) => (
        <IconButton
          label={`Ver ou editar ${provider.name}`}
          title="Ver / Editar"
          onClick={() => onViewDetails?.(provider)}
        >
          <Pencil size={16} />
        </IconButton>
      ),
    },
  ];

  return <DataTable columns={columns} rows={providers} />;
}
