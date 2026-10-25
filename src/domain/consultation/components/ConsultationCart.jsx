// Carrinho de lançamentos: itens adicionados e o botão que grava tudo
// Mostra sempre CART_SLOTS vagas: as que sobram viram placeholders. Passando disso, só a lista rola
import { useEffect, useRef } from "react";
import { CircleCheck, Trash2 } from "lucide-react";
import { Button } from "@/shared/ui/Button";
import { Card } from "@/shared/ui/Card";
import { formatDateTime } from "@/shared/utils/date";
import { formatCurrency } from "@/shared/utils/money";
import styles from "./ConsultationCart.module.css";

const CART_SLOTS = 7;

export function ConsultationCart({
  className,
  items,
  onRemove,
  onFinish,
  isFinishing = false,
  finishedCount = 0,
}) {
  const listRef = useRef(null);
  const previousCount = useRef(items.length);
  const emptySlots = Math.max(CART_SLOTS - items.length, 0);
  const justFinished = items.length === 0 && finishedCount > 0;

  useEffect(() => {
    if (items.length > previousCount.current) {
      listRef.current?.scrollTo({ top: listRef.current.scrollHeight, behavior: "smooth" });
    }
    previousCount.current = items.length;
  }, [items.length]);

  function renderPlaceholder(index) {
    if (index === 0 && justFinished) {
      return (
        <li key="placeholder-0" className={`${styles["cart-slot"]} ${styles["cart-success"]}`} role="status">
          <CircleCheck size={18} aria-hidden />
          {finishedCount === 1 ? "1 lançamento registrado" : `${finishedCount} lançamentos registrados`}
        </li>
      );
    }

    return (
      <li key={`placeholder-${index}`} className={`${styles["cart-slot"]} ${styles["cart-placeholder"]}`}>
        {index === 0 ? "O próximo paciente aparece aqui" : "Espaço livre"}
      </li>
    );
  }

  return (
    <Card title={`Carrinho (${items.length})`} className={[styles["cart-card"], className].filter(Boolean).join(" ")}>
      <ul ref={listRef} className={styles["cart-list"]}>
        {items.map((item) => (
          <li key={item.key} className={`${styles["cart-slot"]} ${styles["cart-item"]}`}>
            <div className={styles["cart-item-info"]}>
              <span className={styles["cart-item-name"]}>{item.patient.name}</span>
              <span className={styles["cart-item-details"]}>
                {item.technique.name} · {formatDateTime(item.arrivalTime)}
              </span>
            </div>
            <span className={styles["cart-item-value"]}>{formatCurrency(item.providerPayoutValue)}</span>
            <button
              type="button"
              className={styles["cart-item-remove"]}
              onClick={() => onRemove(item.key)}
              disabled={isFinishing}
              aria-label={`Remover ${item.patient.name} (${item.technique.name})`}
            >
              <Trash2 size={16} aria-hidden />
            </button>
          </li>
        ))}

        {Array.from({ length: emptySlots }, (_, index) => renderPlaceholder(index))}
      </ul>

      <div className={styles["cart-footer"]}>
        <Button className={styles["cart-finish-button"]} onClick={onFinish} loading={isFinishing} disabled={items.length === 0}>
          Finalizar lançamentos
        </Button>
      </div>
    </Card>
  );
}
