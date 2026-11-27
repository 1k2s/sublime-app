// Dashboard: o rendimento do período (Hoje, Esta semana, Este mês ou Personalizado), pela data do
// lançamento, nas abas "Resumo" (os números e a lista) e "Panorama executivo" (a pizza da
// composição do repasse). Todos os números vêm do backend: a tela não soma.
// O PROVIDER vê só os dele; o ADMIN vê todos e escolhe um prestador na lista ao lado dos números
import { lazy, Suspense, useState } from "react";
import { useAuth } from "@/domain/auth/context/useAuth";
import { ROLES } from "@/domain/auth/roles";
import { PageHeader } from "@/shared/components/PageHeader";
import { PageLoader } from "@/shared/components/PageLoader";
import { useEarnings, useEarningsSummary, useLaunchedConsultations } from "../api/useConsultations";
import { MonitorDashboardIcon, TableIcon } from "../components/DashboardTabIcons";
import { DashboardTabs } from "../components/DashboardTabs";
import { EarningsNumbers } from "../components/EarningsNumbers";
import { LaunchedConsultationsTable } from "../components/LaunchedConsultationsTable";
import { PeriodSelector } from "../components/PeriodSelector";
import { ProviderEarningsList } from "../components/ProviderEarningsList";
import { useDashboardPeriod } from "../hooks/useDashboardPeriod";
import { ALL_STATUSES, filterConsultations } from "../utils/filterConsultations";
import styles from "./DashboardPage.module.css";

// O ECharts só baixa quando o Panorama é aberto
const PayoutCompositionChart = lazy(() =>
  import("../components/PayoutCompositionChart").then((module) => ({ default: module.PayoutCompositionChart }))
);

export function DashboardPage() {
  const { user } = useAuth();
  const isAdmin = user?.role === ROLES.ADMIN.value;
  // Só o ADMIN escolhe o prestador ("" = todos); o PROVIDER é sempre ele mesmo (o backend garante)
  const [providerId, setProviderId] = useState("");
  const readingProviderId = isAdmin ? providerId || undefined : undefined;
  const [status, setStatus] = useState(ALL_STATUSES);
  const [activeTab, setActiveTab] = useState("summary");

  const summaryQuery = useEarningsSummary(readingProviderId);
  const period = useDashboardPeriod(summaryQuery.data);
  const earningsQuery = useEarnings({ providerId: readingProviderId, from: period.from, to: period.to });
  // A lista dos prestadores: o earnings sem prestador. Em "Todos" (e para o PROVIDER) é a mesma
  // chave do de cima, e o TanStack Query faz uma requisição só
  const allProvidersQuery = useEarnings({ providerId: undefined, from: period.from, to: period.to });
  const listQuery = useLaunchedConsultations({ providerId: readingProviderId, from: period.from, to: period.to });

  const consultations = listQuery.data ?? [];
  const visibleConsultations = filterConsultations(consultations, { status });
  // Sem as duas datas do Personalizado, nada é buscado: a tela pede o período
  const hasPeriod = Boolean(period.from && period.to);
  const isEarningsLoading = summaryQuery.isLoading || (hasPeriod && earningsQuery.isLoading);
  const earningsError = summaryQuery.error?.message ?? earningsQuery.error?.message;

  // O conteúdo de cada aba e, para o ADMIN, a lista dos prestadores ao lado, com a mesma altura
  function withProviders(content) {
    if (!isAdmin) return content;
    return (
      <div className={styles.split}>
        {content}
        <ProviderEarningsList
          className={styles.providers}
          earnings={allProvidersQuery.data}
          selectedId={providerId}
          onSelect={setProviderId}
        />
      </div>
    );
  }

  const tabs = [
    {
      value: "summary",
      label: "Resumo",
      icon: MonitorDashboardIcon,
      content: (
        <div className={styles.panel}>
          {withProviders(
            <EarningsNumbers
              earnings={earningsQuery.data}
              isLoading={isEarningsLoading}
              errorMessage={earningsError}
              stacked={isAdmin}
            />
          )}
          <LaunchedConsultationsTable
            consultations={visibleConsultations}
            hasRecords={consultations.length > 0}
            isLoading={summaryQuery.isLoading || (hasPeriod && listQuery.isLoading)}
            isError={listQuery.isError}
            status={status}
            onStatusChange={setStatus}
            showProvider={isAdmin && !providerId}
          />
        </div>
      ),
    },
    {
      value: "panorama",
      label: "Panorama executivo",
      icon: TableIcon,
      content: withProviders(
        <Suspense fallback={<PageLoader variant="section" label="Carregando o Panorama executivo..." />}>
          <PayoutCompositionChart
            earnings={earningsQuery.data}
            isLoading={isEarningsLoading}
            errorMessage={earningsError}
          />
        </Suspense>
      ),
    },
  ];

  return (
    <div className={styles.page}>
      <PageHeader
        title="Dashboard"
        description={
          isAdmin
            ? "O rendimento dos prestadores no período, pela data do lançamento."
            : "O seu rendimento no período, pela data do lançamento."
        }
      />

      <DashboardTabs
        tabs={tabs}
        activeTab={activeTab}
        onChange={setActiveTab}
        toolbar={
          <div className={styles.filters}>
            <PeriodSelector period={period} />
            {!hasPeriod && !summaryQuery.isLoading && (
              <p className={styles.hint}>Escolha as duas datas do período.</p>
            )}
          </div>
        }
      />
    </div>
  );
}
