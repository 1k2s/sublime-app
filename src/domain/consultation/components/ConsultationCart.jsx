// Carrinho de lançamentos: itens adicionados e o botão que grava tudo. O total fica no ConsultationTotal
// Mostra sempre CART_SLOTS vagas: as que sobram viram placeholders. Passando disso, só a lista rola
import { useEffect, useRef } from "react";
import { CircleCheck, Trash2 } from "lucide-react";
import { Button } from "@/shared/ui/Button";
import { Card } from "@/shared/ui/Card";
import { formatDateTime } from "@/shared/utils/date";
import { formatCurrency } from "@/shared/utils/money";
import styles from "./ConsultationCart.module.css";

const CART_SLOTS = 4;

export function ConsultationCart({
  className,
  items,
  onRemove,
  onFinish,
  isFinishing = false,
  finishError,
  finishedCount = 0,
}) {
  const listRef = useRef(null);
  const previousCount = useRef(items.length);
  const emptySlots = Math.max(CART_SLOTS - items.length, 0);
  const justFinished = items.length === 0 && finishedCount > 0;

  // Item novo entra no fim da lista: rola até ele para ficar à vista
  useEffect(() => {
    if (items.length > previousCount.current) {
      listRef.current?.scrollTo({ top: listRef.current.scrollHeight, behavior: "smooth" });
    }
    previousCount.current = items.length;
  }, [items.length]);

  function renderPlaceholder(index) {
    if (index === 0 && justFinished) {
      return (
        <li key="placeholder-0" className={`${styles.slot} ${styles.success}`} role="status">
          <CircleCheck size={18} aria-hidden />
          {finishedCount === 1 ? "1 lançamento registrado" : `${finishedCount} lançamentos registrados`}
        </li>
      );
    }

    return (
      <li key={`placeholder-${index}`} className={`${styles.slot} ${styles.placeholder}`}>
        {index === 0 ? "O próximo paciente aparece aqui" : "Espaço livre"}
      </li>
    );
  }

  return (
    <Card title={`Carrinho (${items.length})`} className={className}>
      <ul ref={listRef} className={styles.list}>
        {items.map((item) => (
          <li key={item.patient.id} className={`${styles.slot} ${styles.item}`}>
            <div className={styles.info}>
              <span className={styles.name}>{item.patient.name}</span>
              <span className={styles.details}>
                {formatDateTime(item.arrivalTime)} · {item.contract.planName}
              </span>
            </div>
            <span className={styles.value}>{formatCurrency(item.providerPayoutValue)}</span>
            <button
              type="button"
              className={styles.remove}
              onClick={() => onRemove(item.patient.id)}
              disabled={isFinishing}
              aria-label={`Remover ${item.patient.name}`}
            >
              <Trash2 size={16} aria-hidden />
            </button>
          </li>
        ))}

        {Array.from({ length: emptySlots }, (_, index) => renderPlaceholder(index))}
      </ul>

      <div className={styles.footer}>
        {finishError && (
          <p className={styles.error} role="alert">
            {finishError.message}
          </p>
        )}

        <Button className={styles.finish} onClick={onFinish} loading={isFinishing} disabled={items.length === 0}>
          Finalizar lançamentos
        </Button>
      </div>
    </Card>
  );
}
