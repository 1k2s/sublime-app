// Tabela de preços de um grupo de atendimento: uma linha por versão do plano, com o status
// (Vigente ou Encerrado) e o filtro por status. O olho abre o detalhe; o da versão vigente leva à
// alteração dos valores. "Novo Preço / Reajustar" cadastra qualquer combinação (ex: um plano ou
// uma duração que o grupo ainda não tem)
import { useState } from "react";
import { Plus, Users } from "lucide-react";
import { Button } from "@/shared/components/Button";
import { FilterPills } from "@/shared/components/FilterPills";
import { FormField } from "@/shared/components/FormField";
import { ListMessage } from "@/shared/components/ListMessage";
import { PageHeader } from "@/shared/components/PageHeader";
import { RichSelect } from "@/shared/components/RichSelect";
import { Skeleton } from "@/shared/components/Skeleton";
import { showError } from "@/shared/utils/alert";
import { PlanPriceDetailModal } from "../components/PlanPriceDetailModal";
import { PlanPriceFormModal } from "../components/PlanPriceFormModal";
import { PriceFormModal } from "../components/PriceFormModal";
import { PriceTable } from "../components/PriceTable";
import { PriceTableSkeleton } from "../components/PriceTableSkeleton";
import { PRICE_STATUS_OPTIONS } from "../display/priceStatus";
import { usePlans, usePricingGroupPriceHistory, usePricingGroups, useSavePlanPrices } from "../api/usePricing";
import { listGroupAxes } from "../utils/planAxes";
import { buildPlanPriceChanges } from "../utils/planPriceChanges";
import { buildPriceRows, filterPriceRows } from "../utils/priceRows";
import styles from "./PricingCatalogPage.module.css";

const TABLE_MESSAGES = {
  loading: "Carregando preços...",
  error: "Não foi possível carregar os preços.",
  filtered: "Nenhum preço com este status.",
  empty: "Nenhum preço cadastrado neste grupo.",
  emptyHint: 'Use "Novo Preço / Reajustar" para cadastrar o primeiro valor.',
};

const GROUP_MESSAGES = {
  ...TABLE_MESSAGES,
  error: "Não foi possível carregar os grupos de atendimento.",
  empty: "Nenhum grupo de atendimento cadastrado.",
  emptyHint: "",
};

export function PricingCatalogPage() {
  const groupsQuery = usePricingGroups();
  const plansQuery = usePlans();
  const savePlanPrices = useSavePlanPrices();

  const [selectedGroupId, setSelectedGroupId] = useState(null);
  const [status, setStatus] = useState("CURRENT");
  // Linha aberta no modal (só a chave: a linha é lida da tabela, que se atualiza depois de salvar)
  // e o modo: "view" (detalhe) ou "edit" (alteração dos valores, só da versão vigente)
  const [openRow, setOpenRow] = useState(null);
  // null = modal fechado; objeto vazio = "Novo Preço / Reajustar"
  const [priceFormValues, setPriceFormValues] = useState(null);

  const groups = groupsQuery.data ?? [];
  const group = groups.find((item) => item.id === selectedGroupId) ?? groups[0];

  const historyQuery = usePricingGroupPriceHistory(group?.id ?? null);
  const rows = buildPriceRows(historyQuery.data?.prices ?? []);
  const visibleRows = filterPriceRows(rows, status);
  const currentRows = rows.filter((row) => row.isCurrent);
  // Colunas: os eixos das linhas visíveis. A versão vigente edita os eixos vigentes do grupo; uma
  // encerrada mostra só os eixos que tinha
  const tableAxes = listGroupAxes(visibleRows);
  const selectedRow = rows.find((row) => row.key === openRow?.key);
  const modalAxes = selectedRow?.isCurrent ? listGroupAxes(currentRows) : listGroupAxes(selectedRow ? [selectedRow] : []);

  const isLoading = groupsQuery.isLoading || historyQuery.isLoading;

  function handleSavePlan(values) {
    const changes = buildPlanPriceChanges({
      plan: selectedRow,
      axes: modalAxes,
      pricingModel: group.pricingModel,
      pricingGroupId: group.id,
      values,
    });
    const backToDetail = () => setOpenRow({ key: selectedRow.key, mode: "view" });
    if (changes.length === 0) {
      backToDetail();
      return;
    }
    savePlanPrices.mutate(changes, {
      onSuccess: backToDetail,
      onError: (error) => showError(error.message),
    });
  }

  function renderTable() {
    if (isLoading) return <PriceTableSkeleton />;
    if (groupsQuery.isError) return <ListMessage isError messages={GROUP_MESSAGES} />;
    if (!group) return <ListMessage messages={GROUP_MESSAGES} />;
    if (historyQuery.isError) return <ListMessage isError messages={TABLE_MESSAGES} />;
    if (visibleRows.length === 0) return <ListMessage hasRecords={rows.length > 0} messages={TABLE_MESSAGES} />;
    return (
      <PriceTable
        rows={visibleRows}
        axes={tableAxes}
        pricingModel={group.pricingModel}
        onView={(key) => setOpenRow({ key, mode: "view" })}
      />
    );
  }

  return (
    <div className={styles.page}>
      <PageHeader
        title="Tabela de Preços"
        description="Gerencie os valores vigentes de cada grupo de atendimento e plano da clínica."
        action={
          <Button onClick={() => setPriceFormValues({})} disabled={groupsQuery.isLoading || groupsQuery.isError}>
            <Plus size={16} aria-hidden="true" /> Novo Preço / Reajustar
          </Button>
        }
      />

      <div className={styles.filters}>
        <div className={styles.groupPicker}>
          {groupsQuery.isLoading ? (
            <Skeleton className={styles.groupSkeleton} />
          ) : (
            groups.length > 0 && (
              <FormField label="Grupo de atendimento" htmlFor="pricingGroup">
                <RichSelect
                  id="pricingGroup"
                  icon={Users}
                  value={group?.id}
                  options={groups.map((item) => ({ value: item.id, label: item.name }))}
                  onChange={setSelectedGroupId}
                />
              </FormField>
            )
          )}
        </div>
        <FilterPills options={PRICE_STATUS_OPTIONS} value={status} onChange={setStatus} />
      </div>

      {renderTable()}

      {selectedRow && openRow.mode === "view" && (
        <PlanPriceDetailModal
          row={selectedRow}
          axes={modalAxes}
          pricingModel={group.pricingModel}
          groupName={group.name}
          onEdit={() => setOpenRow({ key: selectedRow.key, mode: "edit" })}
          onClose={() => setOpenRow(null)}
        />
      )}

      {selectedRow?.isCurrent && openRow.mode === "edit" && (
        <PlanPriceFormModal
          plan={selectedRow}
          axes={modalAxes}
          pricingModel={group.pricingModel}
          groupName={group.name}
          isSaving={savePlanPrices.isPending}
          onSubmit={handleSavePlan}
          onCancel={() => setOpenRow({ key: selectedRow.key, mode: "view" })}
          onClose={() => setOpenRow(null)}
        />
      )}

      {priceFormValues && (
        <PriceFormModal
          groups={groups}
          plans={plansQuery.data ?? []}
          initialValues={priceFormValues}
          onClose={() => setPriceFormValues(null)}
        />
      )}
    </div>
  );
}
