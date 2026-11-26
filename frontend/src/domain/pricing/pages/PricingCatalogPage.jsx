// Tabela de preços: uma linha por técnica e versão do plano, com o status (Vigente, Vencido ou
// Encerrado). Filtros: grupo de atendimento e técnica (os dois em "Todos" mostram o catálogo
// inteiro; a lista de técnicas segue o grupo escolhido, e escolher uma técnica leva o grupo dela)
// e status. O preço é do grupo (ADR 0004): a
// mesma versão se repete em cada técnica do grupo. O olho abre o detalhe; versões abertas (vigente
// ou vencida) levam à alteração dos valores, que vale para o grupo todo. "Novo Preço / Reajustar"
// cadastra qualquer combinação (ex: um plano ou uma duração que o grupo ainda não tem)
import { useState } from "react";
import { Layers, Plus, Users } from "lucide-react";
import { Button } from "@/shared/components/Button";
import { FilterPills } from "@/shared/components/FilterPills";
import { FormField } from "@/shared/components/FormField";
import { ListMessage } from "@/shared/components/ListMessage";
import { PageHeader } from "@/shared/components/PageHeader";
import { RichSelect } from "@/shared/components/RichSelect";
import { showError } from "@/shared/utils/alert";
import { PlanPriceDetailModal } from "../components/PlanPriceDetailModal";
import { PlanPriceFormModal } from "../components/PlanPriceFormModal";
import { PriceFormModal } from "../components/PriceFormModal";
import { PriceTable } from "../components/PriceTable";
import { PRICE_STATUS_OPTIONS } from "../display/priceStatus";
import {
  usePlans,
  usePricingGroupPriceHistories,
  usePricingGroups,
  useSavePlanPrices,
  useTechniques,
} from "../api/usePricing";
import { listGroupAxes } from "../utils/planAxes";
import { buildPlanPriceChanges } from "../utils/planPriceChanges";
import { buildPriceRows } from "../utils/priceRows";
import { buildTechniquePriceRows, filterTechniquePriceRows, listGroupTechniques } from "../utils/techniquePriceRows";
import styles from "./PricingCatalogPage.module.css";

// Valor do "Todos" nos dois filtros
const ALL = "ALL";

const TABLE_MESSAGES = {
  loading: "Carregando preços...",
  error: "Não foi possível carregar os preços.",
  filtered: "Nenhum preço com estes filtros.",
  empty: "Nenhum preço cadastrado.",
  emptyHint: 'Use "Novo Preço / Reajustar" para cadastrar o primeiro valor.',
};

const GROUP_MESSAGES = {
  ...TABLE_MESSAGES,
  error: "Não foi possível carregar os grupos de atendimento.",
  empty: "Nenhum grupo de atendimento cadastrado.",
  emptyHint: "",
};

const TECHNIQUE_MESSAGES = {
  ...TABLE_MESSAGES,
  error: "Não foi possível carregar as técnicas.",
};

// Opções do filtro de técnica: as do grupo escolhido ou, em "Todos", todas, separadas pelo grupo
function buildTechniqueOptions(groups, techniques) {
  const options = groups.flatMap((group) =>
    listGroupTechniques(techniques, group.id).map((technique) => ({
      value: technique.id,
      label: technique.name,
      group: groups.length > 1 ? group.name : undefined,
    })),
  );
  return [{ value: ALL, label: "Todas" }, ...options];
}

export function PricingCatalogPage() {
  const groupsQuery = usePricingGroups();
  const techniquesQuery = useTechniques();
  const plansQuery = usePlans();
  const savePlanPrices = useSavePlanPrices();

  const [groupFilter, setGroupFilter] = useState(ALL);
  const [techniqueFilter, setTechniqueFilter] = useState(ALL);
  const [status, setStatus] = useState("CURRENT");
  // Linha aberta no modal (o grupo e a chave da versão: a linha é lida das linhas do grupo, que se
  // atualizam depois de salvar) e o modo: "view" (detalhe) ou "edit" (alteração dos valores, em
  // versões abertas: vigente ou vencida)
  const [openRow, setOpenRow] = useState(null);
  // null = modal fechado; objeto vazio = "Novo Preço / Reajustar"
  const [priceFormValues, setPriceFormValues] = useState(null);

  const groups = groupsQuery.data ?? [];
  const techniques = techniquesQuery.data ?? [];
  const visibleGroups = groupFilter === ALL ? groups : groups.filter((group) => group.id === groupFilter);

  const historyQueries = usePricingGroupPriceHistories(visibleGroups.map((group) => group.id));
  const rowsByGroupId = new Map(
    visibleGroups.map((group, index) => [group.id, buildPriceRows(historyQueries[index]?.data?.prices ?? [])]),
  );
  const rows = buildTechniquePriceRows({ groups: visibleGroups, rowsByGroupId, techniques });
  const visibleRows = filterTechniquePriceRows(rows, { techniqueId: techniqueFilter, status });

  // Modal: a versão do plano no grupo dela. Versões abertas (vigente ou vencida) editam os eixos
  // abertos do grupo; uma encerrada mostra só os eixos que tinha
  const openGroup = groups.find((group) => group.id === openRow?.groupId);
  const openGroupRows = rowsByGroupId.get(openRow?.groupId) ?? [];
  const selectedRow = openGroupRows.find((row) => row.key === openRow?.key);
  const isSelectedRowOpen = selectedRow != null && selectedRow.status !== "CLOSED";
  const modalAxes = isSelectedRowOpen
    ? listGroupAxes(openGroupRows.filter((row) => row.status !== "CLOSED"))
    : listGroupAxes(selectedRow ? [selectedRow] : []);

  const isLoading =
    groupsQuery.isLoading || techniquesQuery.isLoading || historyQueries.some((query) => query.isLoading);

  function handleGroupChange(value) {
    setGroupFilter(value);
    // A técnica escolhida pode não ser do grupo novo
    setTechniqueFilter(ALL);
  }

  // A técnica leva o grupo junto: o filtro de grupo passa a mostrar o grupo dela (e a lista de
  // técnicas, as do grupo). "Todas" mantém o grupo escolhido
  function handleTechniqueChange(value) {
    setTechniqueFilter(value);
    const technique = techniques.find((item) => item.id === value);
    if (technique) setGroupFilter(technique.pricingGroupId);
  }

  function handleSavePlan(values) {
    const changes = buildPlanPriceChanges({
      plan: selectedRow,
      axes: modalAxes,
      pricingModel: openGroup.pricingModel,
      pricingGroupId: openGroup.id,
      values,
    });
    const backToDetail = () => setOpenRow({ ...openRow, mode: "view" });
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
    if (isLoading) return <ListMessage isLoading messages={TABLE_MESSAGES} />;
    if (groupsQuery.isError) return <ListMessage isError messages={GROUP_MESSAGES} />;
    if (groups.length === 0) return <ListMessage messages={GROUP_MESSAGES} />;
    if (techniquesQuery.isError) return <ListMessage isError messages={TECHNIQUE_MESSAGES} />;
    if (historyQueries.some((query) => query.isError)) return <ListMessage isError messages={TABLE_MESSAGES} />;
    if (visibleRows.length === 0) return <ListMessage hasRecords={rows.length > 0} messages={TABLE_MESSAGES} />;
    return (
      <PriceTable
        rows={visibleRows}
        showGroup={groupFilter === ALL}
        // tableKey: a linha clicada na tabela (uma por técnica), marcada enquanto o detalhe está aberto
        onView={(row) => setOpenRow({ groupId: row.group.id, key: row.rowKey, tableKey: row.key, mode: "view" })}
        selectedRowKey={openRow?.tableKey ?? null}
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
        {groups.length > 0 && (
          <div className={styles.pickers}>
            <div className={styles.picker}>
              <FormField label="Grupo de atendimento" htmlFor="pricingGroup">
                <RichSelect
                  id="pricingGroup"
                  icon={Users}
                  value={groupFilter}
                  options={[
                    { value: ALL, label: "Todos" },
                    ...groups.map((item) => ({ value: item.id, label: item.name })),
                  ]}
                  onChange={handleGroupChange}
                />
              </FormField>
            </div>
            <div className={styles.picker}>
              <FormField label="Técnica" htmlFor="technique">
                <RichSelect
                  id="technique"
                  icon={Layers}
                  value={techniqueFilter}
                  options={buildTechniqueOptions(visibleGroups, techniques)}
                  onChange={handleTechniqueChange}
                  disabled={techniquesQuery.isLoading || techniquesQuery.isError}
                />
              </FormField>
            </div>
          </div>
        )}
        <FilterPills options={PRICE_STATUS_OPTIONS} value={status} onChange={setStatus} />
      </div>

      {renderTable()}

      {selectedRow && openRow.mode === "view" && (
        <PlanPriceDetailModal
          row={selectedRow}
          axes={modalAxes}
          pricingModel={openGroup.pricingModel}
          groupName={openGroup.name}
          onEdit={() => setOpenRow({ ...openRow, mode: "edit" })}
          onClose={() => setOpenRow(null)}
        />
      )}

      {isSelectedRowOpen && openRow.mode === "edit" && (
        <PlanPriceFormModal
          plan={selectedRow}
          axes={modalAxes}
          pricingModel={openGroup.pricingModel}
          groupName={openGroup.name}
          isSaving={savePlanPrices.isPending}
          onSubmit={handleSavePlan}
          onCancel={() => setOpenRow({ ...openRow, mode: "view" })}
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
