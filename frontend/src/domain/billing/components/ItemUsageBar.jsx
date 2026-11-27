import { Badge } from "@/shared/components/Badge";
import { formatCurrency } from "@/shared/utils/money";
import { formatItemLabel } from "../display/accountLabels";
import styles from "./ItemUsageBar.module.css";

// O uso de um item do contrato: o nome como subtítulo e, acima da barra, o usado no início
// ("2 · R$ 334,00") e o contratado no fim ("12 · R$ 2.004,00"). Além do contratado, o trilho fica
// cheio até o contratado e o que passou sai numa extensão laranja, com a quantidade e o valor
// ("+2 · R$ 334,00"). No Grupo, as aulas não têm valor (a mensalidade é que cobra) e não há
// extensão. Os números vêm da API (items da conta)
export function ItemUsageBar({ item }) {
  const hasExtra = item.extraCount > 0;
  const usedText = item.usedValue != null ? ` · ${formatCurrency(item.usedValue)}` : "";
  const unit = item.groupItem ? " aulas" : "";
  // A largura do trilho: a proporção do usado no contratado (só para desenhar)
  const fill = item.contracted > 0 ? Math.min(100, (item.used / item.contracted) * 100) : 0;

  return (
    <div className={styles.item}>
      <div className={styles.title}>
        {formatItemLabel(item)}
        {item.addedInCurrentVersion && <Badge variant="suggestion">Novo no aditivo</Badge>}
      </div>

      {hasExtra ? (
        <div className={styles.extraGrid} style={{ gridTemplateColumns: `${item.contracted}fr ${item.extraCount}fr` }}>
          <div className={styles.scale}>
            <span>
              <strong>{item.used}</strong>
              {usedText}
            </span>
            <span>
              <strong>{item.contracted}</strong> · {formatCurrency(item.contractedValue)}
            </span>
          </div>
          <div className={[styles.scale, styles.scaleExtra].join(" ")}>
            <span>
              <strong>+{item.extraCount}</strong> · {formatCurrency(item.extraValue)}
            </span>
          </div>
          <div className={styles.bar}>
            <div className={styles.fill} style={{ width: "100%" }} />
          </div>
          <div className={[styles.bar, styles.barExtra].join(" ")}>
            <div className={[styles.fill, styles.fillExtra].join(" ")} style={{ width: "100%" }} />
          </div>
        </div>
      ) : (
        <>
          <div className={styles.scale}>
            <span>
              <strong>{item.used}</strong>
              {unit}
              {usedText}
            </span>
            <span>
              <strong>{item.contracted}</strong>
              {unit} · {formatCurrency(item.contractedValue)}
            </span>
          </div>
          <div className={styles.bar}>
            <div className={styles.fill} style={{ width: `${fill}%` }} />
          </div>
        </>
      )}
    </div>
  );
}
