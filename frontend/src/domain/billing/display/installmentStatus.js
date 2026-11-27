import { formatCurrency } from "@/shared/utils/money";

// Situação de cada parcela, como a API resolve (status), com os valores do enum
// InstallmentState.Status do backend. badge: a variante do Badge
const INSTALLMENT_STATUS = {
  PAID: { label: "Paga", badge: "active" },
  OVERDUE: { label: "Atrasada", badge: "inactive" },
  NEXT: { label: "Próxima", badge: "outline" },
  UPCOMING: { label: "A vencer", badge: "neutral" },
  // a que o aditivo parcelou de novo (a linha fica apagada)
  REPLACED: { label: "Substituída pelo aditivo", badge: "neutral" },
  // a que venceria depois do cancelamento do contrato
  CANCELED: { label: "Cancelada", badge: "neutral" },
  // a que ficou em aberto quando a vigência terminou: a conta passa a valer só pelo saldo
  ENDED: { label: "Não cobrada", badge: "neutral" },
};

export function getInstallmentStatus(status) {
  return INSTALLMENT_STATUS[status] ?? { label: status, badge: "neutral" };
}

// Atrasada com parte paga: "Atrasada · R$ 111,34 em aberto" (o remaining vem da API). As outras:
// só o rótulo
export function getInstallmentStatusText(installment) {
  const { label } = getInstallmentStatus(installment.status);
  if (installment.status === "OVERDUE" && Number(installment.paid) > 0) {
    return `${label} · ${formatCurrency(installment.remaining)} em aberto`;
  }
  return label;
}
