// Só para o ADMIN: o rendimento de cada prestador no período (o `byProvider` do earnings sem
// prestador, na ordem do backend), com "Todos" no topo. Clicar numa linha filtra os números, a
// pizza e a lista. A altura é a do pai; a lista rola quando passa dela.
// A barra de cada linha mostra a parte do prestador no rendimento total: é só a largura do
// desenho, nenhum valor é calculado nem exibido (os valores vêm do backend)
import { ChevronRight } from "lucide-react";
import { formatCurrency } from "@/shared/utils/money";
import styles from "./ProviderEarningsList.module.css";

function shareOf(value, total) {
  return Number(total) > 0 ? Math.min(100, (Number(value) / Number(total)) * 100) : 0;
}

// earnings: o earnings sem prestador; selectedId: "" = todos
export function ProviderEarningsList({ earnings, selectedId, onSelect, className }) {
  const providers = earnings?.byProvider ?? [];

  function renderRow({ value, label, payoutTotal, showBar }) {
    const isActive = selectedId === value;
    return (
      <li key={value || "all"}>
        <button
          type="button"
          aria-pressed={isActive}
          className={[styles.row, isActive && styles.active].filter(Boolean).join(" ")}
          onClick={() => onSelect(value)}
        >
          <span className={styles.name}>{label}</span>
          <span className={styles.value}>{formatCurrency(payoutTotal) || "—"}</span>
          <ChevronRight className={styles.chevron} size={16} aria-hidden />
          {showBar && (
            <span className={styles.bar} aria-hidden="true">
              <span className={styles.fill} style={{ width: `${shareOf(payoutTotal, earnings?.payoutTotal)}%` }} />
            </span>
          )}
        </button>
      </li>
    );
  }

  return (
    <section className={[styles.card, className].filter(Boolean).join(" ")} aria-label="Rendimento por prestador">
      <h2 className={styles.title}>Rendimento por prestador</h2>
      <ul className={styles.list}>
        {renderRow({ value: "", label: "Todos", payoutTotal: earnings?.payoutTotal, showBar: false })}
        {providers.map((provider) =>
          renderRow({
            value: String(provider.providerId),
            label: provider.providerName,
            payoutTotal: provider.payoutTotal,
            showBar: true,
          })
        )}
      </ul>
    </section>
  );
}
