// Seleção com opções ricas: cada opção tem título, uma linha de detalhe e etiquetas, e a lista
// pode ser separada em grupos. Fechado, mostra a opção escolhida (título, detalhe e etiquetas).
// Teclado: setas navegam, Enter escolhe, Esc fecha; clicar fora também fecha.
// options: [{ value, label, description?, tags?: string[], group? }] (o grupo vira um título na
// lista quando muda de uma opção para a seguinte). value: o value escolhido (ou "")
import { useEffect, useId, useRef, useState } from "react";
import { Check, ChevronDown } from "lucide-react";
import styles from "./RichSelect.module.css";

export function RichSelect({
  id,
  value,
  options = [],
  placeholder = "Selecione",
  emptyMessage = "Nenhuma opção",
  onChange,
  invalid = false,
  disabled = false,
}) {
  const listId = useId();
  const rootRef = useRef(null);
  const [isOpen, setIsOpen] = useState(false);
  const [highlighted, setHighlighted] = useState(0);

  const selected = options.find((option) => option.value === value) ?? null;
  const activeIndex = Math.min(highlighted, options.length - 1);

  // Clicar fora fecha a lista
  useEffect(() => {
    if (!isOpen) return undefined;
    function handleMouseDown(event) {
      if (!rootRef.current?.contains(event.target)) setIsOpen(false);
    }
    document.addEventListener("mousedown", handleMouseDown);
    return () => document.removeEventListener("mousedown", handleMouseDown);
  }, [isOpen]);

  // A opção destacada pelo teclado fica sempre visível
  useEffect(() => {
    if (isOpen) document.getElementById(`${listId}-${activeIndex}`)?.scrollIntoView({ block: "nearest" });
  }, [isOpen, listId, activeIndex]);

  function open() {
    setHighlighted(Math.max(0, options.findIndex((option) => option.value === value)));
    setIsOpen(true);
  }

  function choose(option) {
    onChange?.(option.value);
    setIsOpen(false);
  }

  function handleKeyDown(event) {
    // Com a lista aberta, o Esc só fecha a lista: não chega ao Modal em volta (que fecharia junto)
    if (event.key === "Escape" && isOpen) {
      event.stopPropagation();
      setIsOpen(false);
      return;
    }
    if (event.key === "Tab") {
      setIsOpen(false);
      return;
    }
    if (event.key === "ArrowDown" || event.key === "ArrowUp") {
      event.preventDefault();
      if (!isOpen) {
        open();
        return;
      }
      const step = event.key === "ArrowDown" ? 1 : -1;
      setHighlighted((activeIndex + step + options.length) % options.length);
      return;
    }
    if (event.key === "Enter" || event.key === " ") {
      event.preventDefault();
      if (!isOpen) open();
      else if (options[activeIndex]) choose(options[activeIndex]);
    }
  }

  return (
    <div ref={rootRef} className={styles.root}>
      <button
        id={id}
        type="button"
        role="combobox"
        aria-haspopup="listbox"
        aria-expanded={isOpen}
        aria-controls={isOpen ? listId : undefined}
        aria-activedescendant={isOpen ? `${listId}-${activeIndex}` : undefined}
        disabled={disabled}
        className={[styles.trigger, isOpen && styles["trigger-open"], invalid && styles["trigger-invalid"]]
          .filter(Boolean)
          .join(" ")}
        onClick={() => (isOpen ? setIsOpen(false) : open())}
        onKeyDown={handleKeyDown}
      >
        {selected ? (
          <>
            <span className={styles["trigger-text"]}>
              <span className={styles["trigger-label"]}>{selected.label}</span>
              {selected.description && <span className={styles["trigger-description"]}>{selected.description}</span>}
            </span>
            {selected.tags?.map((tag) => (
              <span key={tag} className={styles.tag}>
                {tag}
              </span>
            ))}
          </>
        ) : (
          <span className={styles["trigger-placeholder"]}>{placeholder}</span>
        )}
        <ChevronDown size={18} className={styles["trigger-icon"]} aria-hidden />
      </button>

      {isOpen && (
        <ul id={listId} role="listbox" className={styles.list}>
          {options.length === 0 && <li className={styles.empty}>{emptyMessage}</li>}
          {options.map((option, index) => {
            const startsGroup = option.group && option.group !== options[index - 1]?.group;
            const isSelected = option.value === value;
            return (
              <li key={option.value} role="presentation">
                {startsGroup && <div className={styles.group}>{option.group}</div>}
                <div
                  id={`${listId}-${index}`}
                  role="option"
                  aria-selected={isSelected}
                  className={[
                    styles.option,
                    index === activeIndex && styles["option-active"],
                    isSelected && styles["option-selected"],
                  ]
                    .filter(Boolean)
                    .join(" ")}
                  onMouseEnter={() => setHighlighted(index)}
                  onMouseDown={(event) => event.preventDefault()}
                  onClick={() => choose(option)}
                >
                  <span className={styles["option-text"]}>
                    <span className={styles["option-label"]}>
                      {option.label}
                      {isSelected && <Check size={14} strokeWidth={3} className={styles["option-check"]} aria-hidden />}
                    </span>
                    {option.description && <span className={styles["option-description"]}>{option.description}</span>}
                  </span>
                  {option.tags?.length > 0 && (
                    <span className={styles.tags}>
                      {option.tags.map((tag) => (
                        <span key={tag} className={styles.tag}>
                          {tag}
                        </span>
                      ))}
                    </span>
                  )}
                </div>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
