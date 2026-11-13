// Liberação de lançamentos retroativos, no detalhe do prestador: a liberação de hoje (se houver)
// e o campo para liberar uma quantidade. O prestador só lança com a data de hoje; com a
// liberação, lança essa quantidade em datas passadas até 23:59 de hoje
import { useState } from "react";
import { CalendarClock } from "lucide-react";
import { Button } from "@/shared/components/Button";
import { Input } from "@/shared/components/Input";
import { allowedCountSchema } from "../schemas/retroactiveGrantSchema";
import styles from "./RetroactiveGrantSection.module.css";

export function RetroactiveGrantSection({ grant, isLoading = false, onGrant, isGranting = false }) {
  const [count, setCount] = useState("1");
  const [error, setError] = useState(null);

  function handleGrant() {
    const result = allowedCountSchema.safeParse(count);
    if (!result.success) {
      setError(result.error.issues[0].message);
      return;
    }
    setError(null);
    onGrant(result.data);
  }

  function handleKeyDown(event) {
    if (event.key === "Enter") {
      event.preventDefault();
      handleGrant();
    }
  }

  return (
    <div className={styles.section}>
      <h4 className={styles.title}>
        <CalendarClock size={14} aria-hidden /> Lançamento retroativo
      </h4>

      <p className={styles.status}>
        {isLoading && "Carregando a liberação..."}
        {!isLoading && grant && (
          <>
            Liberado hoje: <strong>restam {grant.remaining} de {grant.allowedCount}</strong>, até 23:59.
          </>
        )}
        {!isLoading && !grant && "Sem liberação de retroativo hoje."}
      </p>

      <div className={styles.form}>
        <label className={styles.label} htmlFor="retroactive-grant-count">
          Quantidade
        </label>
        <Input
          id="retroactive-grant-count"
          type="number"
          min="1"
          max="50"
          className={styles.count}
          value={count}
          invalid={Boolean(error)}
          onChange={(event) => setCount(event.target.value)}
          onKeyDown={handleKeyDown}
        />
        <Button variant="secondary" onClick={handleGrant} loading={isGranting}>
          {grant ? "Liberar de novo" : "Liberar"}
        </Button>
        {grant && <span className={styles.hint}>substitui a atual</span>}
      </div>
      {error && (
        <span className={styles.error} role="alert">
          {error}
        </span>
      )}

      <p className={styles.note}>
        O prestador poderá fazer essa quantidade de lançamentos retroativos (em datas passadas)
        até 23:59 de hoje. Desfazer um deles não devolve a unidade.
      </p>
    </div>
  );
}
