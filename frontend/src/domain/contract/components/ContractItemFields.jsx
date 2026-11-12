import { AlertTriangle, Trash2 } from "lucide-react";
import { Select } from "@/shared/components/Select";
import { formatCurrency } from "@/shared/utils/money";
import { SESSION_FREQUENCY_OPTIONS } from "../sessionFrequencies";
import styles from "./ContractItemFields.module.css";

// Valores distintos de um eixo nas linhas da técnica (todas as linhas, de todos os planos): as
// opções não dependem do plano, então trocar o plano não apaga as escolhas
function distinctValues(prices, axis) {
  const values = prices.map((price) => price[axis]).filter((value) => value != null);
  return [...new Set(values)].sort((a, b) => a - b);
}

// Cabeçalho das colunas dos itens: os rótulos aparecem uma vez, e cada item é uma linha
export function ContractItemsHeader() {
  return (
    <div className={[styles.row, styles.header].join(" ")} aria-hidden>
      <span>Técnica</span>
      <span>Duração / plano do Grupo</span>
      <span>Frequência</span>
      <span className={styles["header-value"]}>Valor da sessão</span>
      <span />
    </div>
  );
}

// Um campo da linha: o select e, embaixo, o erro dele
function Cell({ error, children }) {
  return (
    <div className={styles.cell}>
      {children}
      {error && (
        <span className={styles.error} role="alert">
          {error}
        </span>
      )}
    </div>
  );
}

// Um item do contrato (um por grupo de preço), numa linha. A técnica define o resto:
// - item por duração: duração e frequência;
// - Pilates em Grupo: plano do Grupo (no pacote, as opções da prévia da vigência; na mensalidade,
//   o próprio plano do contrato) e frequência semanal.
// A linha de preço, o valor e o aviso vêm prontos do formulário (resolveItemLine); as sessões e
// o total do item, da prévia do backend (itemValue). As listas só oferecem opções válidas, como
// ajuda: o backend confere de novo
export function ContractItemFields({
  index,
  item = {},
  errors = {},
  register,
  setValue,
  techniqueOptions,
  prices,
  isPricesLoading = false,
  isPricesError = false,
  plan,
  groupPlanOptions = [],
  suggestedGroupPlanId,
  isPreviewLoading = false,
  resolution = {},
  itemValue = null,
  canRemove = false,
  onRemove,
}) {
  const name = (field) => `items.${index}.${field}`;
  const position = `do item ${index + 1}`;

  const isGroup = item.pricingModel === "FREQUENCY_BASED";
  const isDuration = item.pricingModel === "DURATION_BASED";
  const durations = prices ? distinctValues(prices, "durationMinutes") : [];
  const weeklyFrequencies = prices ? distinctValues(prices, "weeklyFrequency") : [];

  // Trocar a técnica muda o grupo: o resto do item deixa de valer
  const techniqueField = register(name("techniqueId"), {
    onChange: (event) => {
      const technique = techniqueOptions.find((option) => String(option.id) === event.target.value);
      setValue(name("pricingModel"), technique?.pricingModel ?? "");
      setValue(name("durationMinutes"), "");
      setValue(name("frequency"), "");
      setValue(name("groupPlanId"), "");
      setValue(name("weeklyFrequency"), "");
    },
  });

  function axisPlaceholder() {
    if (!item.techniqueId) return "Escolha a técnica";
    if (isPricesLoading) return "Carregando...";
    return isPricesError ? "Não foi possível carregar" : "Selecione";
  }

  function groupPlanCell() {
    // Na mensalidade, o plano do Grupo é o próprio plano do contrato
    if (plan?.type !== "PACKAGE") {
      return <span className={styles.fixed}>{plan ? plan.name : "Escolha o plano"}</span>;
    }
    return (
      <Cell error={errors.groupPlanId?.message}>
        <Select
          aria-label={`Plano do Grupo ${position}`}
          disabled={groupPlanOptions.length === 0}
          invalid={Boolean(errors.groupPlanId)}
          {...register(name("groupPlanId"))}
        >
          <option value="">{isPreviewLoading ? "Calculando..." : "Plano do Grupo"}</option>
          {groupPlanOptions.map((option) => (
            <option key={option.id} value={String(option.id)}>
              {option.name}
              {option.id === suggestedGroupPlanId ? " (sugerido)" : ""}
            </option>
          ))}
        </Select>
      </Cell>
    );
  }

  return (
    <div className={styles.item}>
      <div className={styles.row}>
        <Cell error={errors.techniqueId?.message}>
          <Select aria-label={`Técnica ${position}`} invalid={Boolean(errors.techniqueId)} {...techniqueField}>
            <option value="">Selecione a técnica</option>
            {techniqueOptions.map((technique) => (
              <option key={technique.id} value={String(technique.id)}>
                {technique.name}
              </option>
            ))}
          </Select>
        </Cell>

        {isGroup ? (
          <>
            {groupPlanCell()}
            <Cell error={errors.weeklyFrequency?.message}>
              <Select
                aria-label={`Frequência semanal ${position}`}
                disabled={weeklyFrequencies.length === 0}
                invalid={Boolean(errors.weeklyFrequency)}
                {...register(name("weeklyFrequency"))}
              >
                <option value="">{axisPlaceholder()}</option>
                {weeklyFrequencies.map((times) => (
                  <option key={times} value={String(times)}>
                    {times}x por semana
                  </option>
                ))}
              </Select>
            </Cell>
          </>
        ) : (
          <>
            <Cell error={errors.durationMinutes?.message}>
              <Select
                aria-label={`Duração ${position}`}
                disabled={durations.length === 0}
                invalid={Boolean(errors.durationMinutes)}
                {...register(name("durationMinutes"))}
              >
                <option value="">{axisPlaceholder()}</option>
                {durations.map((minutes) => (
                  <option key={minutes} value={String(minutes)}>
                    {minutes} min
                  </option>
                ))}
              </Select>
            </Cell>
            <Cell error={errors.frequency?.message}>
              <Select
                aria-label={`Frequência ${position}`}
                disabled={!isDuration}
                invalid={Boolean(errors.frequency)}
                {...register(name("frequency"))}
              >
                <option value="">Selecione</option>
                {SESSION_FREQUENCY_OPTIONS.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </Select>
            </Cell>
          </>
        )}

        <div className={styles.value}>
          {resolution.line ? (
            <>
              <span className={styles["value-amount"]}>{formatCurrency(resolution.line.sessionValue)}</span>
              {itemValue && (
                <span className={styles["value-total"]}>
                  {itemValue.sessionCount} {isGroup ? "aulas" : "sessões"} · {formatCurrency(itemValue.totalValue)}
                </span>
              )}
            </>
          ) : (
            <span className={styles["value-empty"]}>—</span>
          )}
        </div>

        {canRemove ? (
          <button
            type="button"
            className={styles.remove}
            onClick={onRemove}
            aria-label={`Remover o item ${index + 1}`}
            title="Remover item"
          >
            <Trash2 size={16} aria-hidden />
          </button>
        ) : (
          <span />
        )}
      </div>

      {resolution.issue && (
        <p className={styles.issue}>
          <AlertTriangle size={16} aria-hidden /> {resolution.issue}
        </p>
      )}
    </div>
  );
}
