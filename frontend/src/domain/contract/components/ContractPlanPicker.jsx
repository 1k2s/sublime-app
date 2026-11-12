import { useState } from "react";
import { RichSelect } from "@/shared/components/RichSelect";
import { findPlanCounterpart, isFamilyPlan } from "../planFamily";
import styles from "./ContractPlanPicker.module.css";

// O plano do contrato: um seletor com os planos separados pelo tipo, pacote (sessões; itens por
// duração e, junto, o Pilates em Grupo) e mensalidade (o prazo em semanas). Ver "Plan" no
// domain-model. A etiqueta é a quantidade do plano, não a técnica: assim nada muda quando o
// catálogo ganha técnicas. O alternador Individual | Familiar mostra uma das duas versões de cada
// plano
const PLAN_GROUPS = [
  { type: "PACKAGE", title: "Pacotes", amount: (plan) => `${plan.sessionCount} sessões` },
  { type: "MONTHLY_FEE", title: "Mensalidades", amount: (plan) => `${plan.termWeeks} semanas` },
];

const MODES = [
  { label: "Individual", family: false },
  { label: "Familiar", family: true },
];

function toOptions(plans, family) {
  return PLAN_GROUPS.flatMap((group) =>
    plans
      .filter((plan) => plan.type === group.type && isFamilyPlan(plan) === family)
      .map((plan) => ({
        value: String(plan.id),
        label: plan.name,
        tags: [group.amount(plan)],
        group: group.title,
      })),
  );
}

// plans: os contratáveis (ativos, sem o Avulso). value: o id escolhido, como texto.
// onChange(plan | null): null quando o plano não tem versão no outro modo
export function ContractPlanPicker({ id, plans = [], value, onChange, invalid = false }) {
  const [isFamily, setIsFamily] = useState(false);
  const selectedPlan = plans.find((plan) => String(plan.id) === value) ?? null;

  // Trocar o modo leva o plano escolhido para a versão correspondente (Evolução ↔ Evolução
  // Familiar); sem versão no outro modo, o plano é limpo. Os itens guardam as escolhas, então
  // só os valores mudam
  function changeMode(family) {
    if (family === isFamily) return;
    setIsFamily(family);
    if (selectedPlan) onChange?.(findPlanCounterpart(selectedPlan, plans, family));
  }

  return (
    <div className={styles.picker}>
      <div className={styles.header}>
        <label htmlFor={id} className={styles.label}>
          Plano *
        </label>
        <div role="radiogroup" aria-label="Tipo de plano" className={styles.modes}>
          {MODES.map((mode) => (
            <button
              key={mode.label}
              type="button"
              role="radio"
              aria-checked={isFamily === mode.family}
              className={[styles.mode, isFamily === mode.family && styles["mode-active"]].filter(Boolean).join(" ")}
              onClick={() => changeMode(mode.family)}
            >
              {mode.label}
            </button>
          ))}
        </div>
      </div>

      <RichSelect
        id={id}
        value={value}
        options={toOptions(plans, isFamily)}
        placeholder="Selecione o plano"
        emptyMessage="Nenhum plano disponível"
        invalid={invalid}
        onChange={(planId) => onChange?.(plans.find((plan) => String(plan.id) === planId) ?? null)}
      />
    </div>
  );
}
