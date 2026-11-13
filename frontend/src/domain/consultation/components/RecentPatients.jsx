// Pacientes recentes do prestador: um clique preenche o paciente e sugere a técnica e a duração
// do último atendimento dele (o backend escolhe os recentes)
import styles from "./RecentPatients.module.css";

export function RecentPatients({ recents = [], selectedPatientId, onPick }) {
  if (recents.length === 0) return null;

  return (
    <div className={styles.recents}>
      <span className={styles.label}>Recentes:</span>
      {recents.map((recent) => (
        <button
          key={recent.patientId}
          type="button"
          className={[styles.chip, String(recent.patientId) === selectedPatientId && styles.picked]
            .filter(Boolean)
            .join(" ")}
          title={`Último: ${recent.techniqueName} · ${recent.durationMinutes} min`}
          onClick={() => onPick(recent)}
        >
          {recent.patientName}
        </button>
      ))}
    </div>
  );
}
