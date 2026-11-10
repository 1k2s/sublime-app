// Escolha do contrato quando o paciente tem dois vinculados na data (um como titular, um como beneficiário).
// Só mostra o que a API resolveu: o front não decide de qual contrato a sessão sai
import { Modal } from "@/shared/ui/Modal";
import { Button } from "@/shared/ui/Button";
import { formatDate } from "@/shared/utils/date";
import styles from "./ContractChoiceModal.module.css";

const PRICING_SOURCE_LABELS = {
  CONTRACT_ITEM: "Debita uma sessão deste contrato",
  EQUIVALENT_PLAN: "Não cobre esta técnica: preço pelo plano equivalente, sem debitar sessão",
};

export function ContractChoiceModal({ patientName, contracts, onChoose, onClose }) {
  return (
    <Modal title="De qual contrato sai a sessão?" onClose={onClose}>
      <p className={styles["choice-intro"]}>
        {patientName} tem dois contratos vigentes nesta data. Escolha um.
      </p>

      <ul className={styles["choice-list"]}>
        {contracts.map((contract) => (
          <li key={contract.contractId}>
            <button type="button" className={styles["choice-option"]} onClick={() => onChoose(contract)}>
              <span className={styles["choice-title"]}>
                {contract.planName} · {contract.patientIsHolder ? "titular" : `beneficiário de ${contract.holderName}`}
              </span>
              <span className={styles["choice-detail"]}>{PRICING_SOURCE_LABELS[contract.pricingSource]}</span>
              <span className={styles["choice-detail"]}>
                Válido até {formatDate(contract.endDate)}
                {contract.remainingSessions != null && ` · saldo: ${contract.remainingSessions} sessões`}
              </span>
            </button>
          </li>
        ))}
      </ul>

      <div className={styles["choice-footer"]}>
        <Button variant="secondary" onClick={onClose}>
          Cancelar
        </Button>
      </div>
    </Modal>
  );
}
