import { useState } from "react";
import { FileText, XCircle } from "lucide-react";
import { formatPriceAxis } from "@/domain/pricing/priceAxis";
import { Badge } from "@/shared/components/Badge";
import { Button } from "@/shared/components/Button";
import { Modal } from "@/shared/components/Modal";
import { formatDate } from "@/shared/utils/date";
import { formatCurrency } from "@/shared/utils/money";
import { getContractStatus } from "../contractStatus";
import { formatInstallments } from "../formatInstallments";
import { getPaymentMethodLabel } from "../paymentMethods";
import { getSessionFrequencyLabel } from "../sessionFrequencies";
import styles from "./ContractDetailsModal.module.css";

// Detalhe do contrato: cabeçalho e itens (com o valor travado de cada um). Só leitura: o
// aditivo (prorrogar, incluir e remover itens) é outra feature. contract: já com patientName e
// beneficiaryName (withPatientNames)
export function ContractDetailsModal({ contract, isDeactivating = false, onDeactivate, onClose }) {
  const [isConfirming, setIsConfirming] = useState(false);

  if (!contract) return null;

  const status = getContractStatus(contract);

  function confirmDeactivate() {
    onDeactivate?.(contract);
    setIsConfirming(false);
  }

  // O componente continua montado com o modal fechado: sem zerar, a confirmação reapareceria
  // aberta no próximo contrato
  function close() {
    setIsConfirming(false);
    onClose?.();
  }

  const fields = [
    { label: "Titular", value: contract.patientName },
    { label: "Beneficiário", value: contract.beneficiaryName ?? "Nenhum" },
    { label: "Plano", value: contract.planName },
    { label: "Forma de pagamento", value: getPaymentMethodLabel(contract.paymentMethod) },
    { label: "Início", value: formatDate(contract.startDate) },
    { label: "Fim", value: formatDate(contract.endDate) },
    // Calculados pela API a partir das linhas travadas nos itens
    { label: "Valor total", value: formatCurrency(contract.totalValue) },
    { label: "Parcelas", value: formatInstallments(contract) },
  ];

  // Inativar só contrato ativo (vigente ou vencido): contrato não tem reativação.
  // Durante a confirmação, o rodapé sai: as ações ficam na caixa de confirmação
  const footer = isConfirming ? null : (
    <div className={styles.footer}>
      <div>
        {contract.active && (
          <Button variant="danger" onClick={() => setIsConfirming(true)} disabled={isDeactivating}>
            <XCircle size={16} /> Inativar contrato
          </Button>
        )}
      </div>
      <Button variant="outline" onClick={close}>
        Fechar
      </Button>
    </div>
  );

  return (
    <Modal
      title={contract.patientName ?? "Contrato"}
      icon={FileText}
      titleExtra={<Badge variant={status.badge}>{status.label}</Badge>}
      description={`Contrato nº ${contract.id}`}
      footer={footer}
      onClose={close}
    >
      <div className={styles.content}>
        <dl className={styles.grid}>
          {fields.map((field) => (
            <div key={field.label} className={styles.field}>
              <dt className={styles.label}>{field.label}</dt>
              <dd className={styles.value}>{field.value || "--"}</dd>
            </div>
          ))}
        </dl>

        <section className={styles.items}>
          <h4 className={styles.sectionTitle}>Itens</h4>
          <ul className={styles.itemList}>
            {contract.items.map((item) => (
              <li key={item.id} className={styles.item}>
                <div className={styles.itemText}>
                  <span className={styles.itemTitle}>
                    {item.techniqueName} <span className={styles.itemGroup}>· {item.pricingGroupName}</span>
                  </span>
                  <span className={styles.itemDetail}>
                    {[item.planName, formatPriceAxis(item), item.frequency && getSessionFrequencyLabel(item.frequency)]
                      .filter(Boolean)
                      .join(" · ")}
                  </span>
                </div>
                <div className={styles.itemValues}>
                  <span className={styles.itemValue}>{formatCurrency(item.sessionValue)} por sessão</span>
                  <span className={styles.itemTotal}>
                    {item.sessionCount} {item.sessionFrequencyPriceId ? "aulas" : "sessões"} ·{" "}
                    {formatCurrency(item.totalValue)}
                  </span>
                </div>
              </li>
            ))}
          </ul>
        </section>

        {isConfirming && (
          <div className={styles.confirmBox}>
            <p className={styles.confirmText}>
              Inativar este contrato? Não tem volta: contrato não pode ser reativado. Use só para corrigir um
              contrato criado errado (ex: plano ou titular errado).
            </p>
            <div className={styles.actions}>
              <Button variant="outline" onClick={() => setIsConfirming(false)}>
                Cancelar
              </Button>
              <Button variant="danger" onClick={confirmDeactivate} disabled={isDeactivating}>
                Confirmar inativação
              </Button>
            </div>
          </div>
        )}
      </div>
    </Modal>
  );
}
