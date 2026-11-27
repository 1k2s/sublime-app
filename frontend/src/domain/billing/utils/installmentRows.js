// As linhas da tabela de parcelas de uma versão: as parcelas seguidas que o aditivo substituiu
// (status REPLACED) viram uma linha só ("7ª a 12ª · substituídas pelo aditivo"); as outras ficam
// uma por linha. Função pura, sobre as parcelas da API
export function toInstallmentRows(installments = []) {
  const rows = [];
  for (const installment of installments) {
    const last = rows.at(-1);
    if (installment.status === "REPLACED" && last?.kind === "replaced") {
      last.installments.push(installment);
    } else if (installment.status === "REPLACED") {
      rows.push({ kind: "replaced", key: `replaced-${installment.number}`, installments: [installment] });
    } else {
      rows.push({ kind: "installment", key: `installment-${installment.number}`, installment });
    }
  }
  return rows;
}
