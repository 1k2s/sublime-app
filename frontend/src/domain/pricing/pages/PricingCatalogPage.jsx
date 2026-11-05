// Tabela de preços vigentes, filtrada por grupo de preço
import { useState } from "react";
import { Plus } from "lucide-react";
import { Button } from "@/shared/ui/Button";
import { PageHeader } from "@/shared/ui/PageHeader";
import { PriceFormModal } from "../components/PriceFormModal";
import { PriceTable } from "../components/PriceTable";
import { usePlans, usePricingGroups, useTechniquePrices, useTechniques } from "../api/usePricing";
import styles from "./PricingCatalogPage.module.css";

export function PricingCatalogPage() {
  const groupsQuery = usePricingGroups();
  const plansQuery = usePlans();
  const techniquesQuery = useTechniques();

  const [selectedGroupId, setSelectedGroupId] = useState(null);
  // null = modal fechado; objeto vazio = preço novo; com valores = reajuste de uma linha
  const [modalValues, setModalValues] = useState(null);

  const groups = groupsQuery.data ?? [];
  const group = groups.find((item) => item.id === selectedGroupId) ?? groups[0];

  // O backend só expõe os preços vigentes a partir de uma técnica; qualquer técnica do grupo serve
  const technique = (techniquesQuery.data ?? []).find((item) => item.pricingGroupId === group?.id);
  const pricesQuery = useTechniquePrices(technique?.id ?? null);
  const prices = pricesQuery.data?.prices ?? [];

  const isLoading = groupsQuery.isLoading || plansQuery.isLoading || techniquesQuery.isLoading;
  const loadError = groupsQuery.error || plansQuery.error || techniquesQuery.error || pricesQuery.error;

  function openEdit(price) {
    setModalValues({
      pricingGroupId: String(group.id),
      planId: String(price.planId),
      axis: String(price.durationMinutes ?? price.weeklyFrequency),
      sessionValue: String(price.sessionValue),
    });
  }

  function renderContent() {
    if (isLoading) return <p className={styles.message}>Carregando...</p>;
    if (loadError) return <p className={styles.message}>Não foi possível carregar os preços.</p>;
    if (!group) return <p className={styles.message}>Nenhum grupo de preço cadastrado.</p>;
    if (!technique) return <p className={styles.message}>Este grupo ainda não tem técnica cadastrada.</p>;
    if (pricesQuery.isLoading) return <p className={styles.message}>Carregando...</p>;
    if (prices.length === 0) return <p className={styles.message}>Nenhum preço vigente neste grupo.</p>;
    return <PriceTable prices={prices} pricingModel={group.pricingModel} onEdit={openEdit} />;
  }

  return (
    <div>
      <PageHeader
        className={styles.header}
        title="Tabela de Preços"
        description="Gerencie os valores vigentes de cada grupo de atendimento e plano da clínica."
        action={
          <Button onClick={() => setModalValues({})} disabled={isLoading || !!loadError}>
            <Plus size={16} aria-hidden="true" /> Novo Preço / Reajustar
          </Button>
        }
      />

      <div className={styles.tabs} role="tablist">
        {groups.map((item) => (
          <button
            key={item.id}
            type="button"
            role="tab"
            aria-selected={item.id === group?.id}
            className={`${styles.tab} ${item.id === group?.id ? styles.active : ""}`}
            onClick={() => setSelectedGroupId(item.id)}
          >
            {item.name}
          </button>
        ))}
      </div>

      {renderContent()}

      {modalValues && (
        <PriceFormModal
          groups={groups}
          plans={plansQuery.data ?? []}
          initialValues={modalValues}
          onClose={() => setModalValues(null)}
        />
      )}
    </div>
  );
}
