import { formatDate } from "@/shared/utils/date";
import { formatCurrency } from "@/shared/utils/money";
import { formatContractLabel } from "../display/accountLabels";
import { formatBalance, getBalanceNote, getBalanceTone } from "../display/accountStatus";
import { formatWarning } from "../display/accountWarnings";
import { AccountCells } from "./AccountCells";
import { ItemUsageBar } from "./ItemUsageBar";
import styles from "./ContractAccountCard.module.css";

function plural(count, singular, pluralForm) {
  return `${count} ${count === 1 ? singular : pluralForm}`;
}

// O consumido do cartão é o devido (o consumo + as mensalidades vencidas do Grupo): assim as
// quatro células fecham (pago − consumido = saldo). Tudo vem pronto da API
function consumedNote(account) {
  const sessions = plural(account.sessionsUsed, "sessão", "sessões");
  return Number(account.groupFeesDue) > 0 ? `${sessions} + ${formatCurrency(account.groupFeesDue)} do Grupo` : sessions;
}

// O subtítulo do cartão, pelo status do contrato (da API): a vigência, o vencido com pendência, o
// encerrado ou o cancelado
function subtitleOf(account) {
  const endDate = formatDate(account.endDate);
  switch (account.contractStatus) {
    case "EXPIRED":
      return `vencido em ${endDate} · com pendência`;
    case "CLOSED":
      return `encerrado em ${endDate}`;
    case "CANCELED":
      return "contrato cancelado";
    default:
      return `vigência até ${endDate}`;
  }
}

// A nota do pago: o % do total e, com transferências de saldo, o crédito recebido ou o enviado
function paidNote(account) {
  return [
    `${account.paidPercentage}% do total`,
    Number(account.creditReceived) > 0 && `inclui ${formatCurrency(account.creditReceived)} de crédito`,
    Number(account.creditTransferred) > 0 && `${formatCurrency(account.creditTransferred)} passados a outro contrato`,
  ]
    .filter(Boolean)
    .join(" · ");
}

// A linha de atalho das parcelas: "1 atrasada · próxima 10/12 · R$ 779,33" ou "em dia · ..."
function InstallmentsLine({ installments, onViewInstallments }) {
  const { paidCount, totalCount, overdueCount, next } = installments;
  return (
    <div className={[styles.section, styles.installments].join(" ")}>
      <div className={styles.installmentsText}>
        <span className={styles.label}>Parcelas</span>
        <span className={styles.installmentsValue}>
          {overdueCount > 0 ? (
            <strong className={styles.negative}>{plural(overdueCount, "atrasada", "atrasadas")}</strong>
          ) : (
            "em dia"
          )}
          {next && ` · próxima ${formatDate(next.dueDate, "dd/MM")} · ${formatCurrency(next.amount)}`}
          {!next && overdueCount === 0 && paidCount === totalCount && " · todas pagas"}
        </span>
      </div>
      <button type="button" className={styles.link} onClick={onViewInstallments}>
        Ver parcelas
      </button>
    </div>
  );
}

// O cartão de um contrato no Resumo da conta: o plano e a versão, as quatro células (total do
// contrato, pago, consumido e saldo), a linha das parcelas, os avisos e o uso de cada item
export function ContractAccountCard({ account, onViewInstallments }) {
  const cells = [
    {
      label: "Total do contrato",
      value: formatCurrency(account.totalValue),
      note: `em ${plural(account.installments.totalCount, "parcela", "parcelas")}`,
    },
    { label: "Pago", value: formatCurrency(account.paid), note: paidNote(account) },
    { label: "Consumido", value: formatCurrency(account.due), note: consumedNote(account) },
    {
      label: "Saldo",
      value: formatBalance(account.balance),
      note: getBalanceNote(account.balance),
      tone: getBalanceTone(account.balance),
    },
  ];

  return (
    <div className={styles.card}>
      <AccountCells
        title={formatContractLabel(account)}
        subtitle={subtitleOf(account)}
        cells={cells}
      />

      <InstallmentsLine installments={account.installments} onViewInstallments={onViewInstallments} />

      {account.warnings.length > 0 && (
        <ul className={styles.warnings}>
          {account.warnings.map((warning) => (
            <li key={`${warning.type}-${warning.techniqueName ?? ""}`}>{formatWarning(warning)}</li>
          ))}
        </ul>
      )}

      {account.items.length > 0 && (
        <div className={styles.section}>
          {account.items.map((item) => (
            <ItemUsageBar key={item.pricingGroupId} item={item} />
          ))}
        </div>
      )}
    </div>
  );
}
