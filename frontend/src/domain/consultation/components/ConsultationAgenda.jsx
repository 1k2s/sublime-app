// Folha de agenda que mostra o lançamento sendo montado: paciente, técnica e duração, cada um com seu ícone
// Ocupa só a altura que o pai der: linhas de altura fixa e animações só de transform/opacity
import { Activity, Check, User } from "lucide-react";
import { TechniqueIcon } from "@/domain/pricing/components/TechniqueIcon";
import { ClockIcon } from "@/shared/ui/ClockIcon";
import { formatDate } from "@/shared/utils/date";
import styles from "./ConsultationAgenda.module.css";

function initials(name) {
  const parts = name.trim().split(/\s+/);
  const last = parts.length > 1 ? parts[parts.length - 1][0] : "";
  return `${parts[0][0]}${last}`.toUpperCase();
}

export function ConsultationAgenda({ patient, technique, durationMinutes, occurredAt, className }) {
  // O relógio mostra a duração como se fosse um horário (ex: 50 min -> ponteiro nos 50)
  const totalMinutes = Number(durationMinutes) || 0;

  const rows = [
    {
      label: "Paciente",
      value: patient?.name,
      icon: patient ? (
        <span key={patient.id} className={styles["agenda-avatar"]}>
          {initials(patient.name)}
        </span>
      ) : (
        <User size={18} aria-hidden />
      ),
    },
    {
      label: "Técnica",
      value: technique?.name,
      icon: technique ? <TechniqueIcon key={technique.id} name={technique.name} /> : <Activity size={18} aria-hidden />,
    },
    {
      label: "Duração",
      value: totalMinutes > 0 ? `${totalMinutes} min` : "",
      icon: <ClockIcon hours={Math.floor(totalMinutes / 60)} minutes={totalMinutes % 60} />,
    },
  ];

  const firstPending = rows.findIndex((row) => !row.value);
  const completed = firstPending === -1 ? rows.length : firstPending;
  const isComplete = completed === rows.length;

  function rowStatus(row, index) {
    if (row.value) return styles["agenda-row-done"];
    if (index === completed) return styles["agenda-row-current"];
    return "";
  }

  return (
    <div
      role="progressbar"
      aria-label="Progresso do lançamento"
      aria-valuemin={0}
      aria-valuemax={rows.length}
      aria-valuenow={completed}
      aria-valuetext={isComplete ? "Todas as informações preenchidas" : `Falta ${rows[completed].label}`}
      className={[styles["agenda"], isComplete && styles["agenda-complete"], className].filter(Boolean).join(" ")}
    >
      <div className={styles["agenda-header"]}>
        <span className={styles["agenda-date"]}>{formatDate(occurredAt || new Date(), "EEEE, d 'de' MMMM")}</span>
        <span className={styles["agenda-status"]}>
          <Check size={14} strokeWidth={3} aria-hidden />
          Pronto
        </span>
      </div>

      <ul className={styles["agenda-rows"]}>
        {rows.map((row, index) => (
          <li key={row.label} className={`${styles["agenda-row"]} ${rowStatus(row, index)}`}>
            <span className={styles["agenda-tile"]}>{row.icon}</span>
            <span className={styles["agenda-text"]}>
              <span className={styles["agenda-label"]}>{row.label}</span>
              {row.value && (
                <span key={row.value} className={styles["agenda-value"]}>
                  {row.value}
                </span>
              )}
            </span>
          </li>
        ))}
      </ul>
    </div>
  );
}
