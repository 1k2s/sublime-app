// Campo de digitar e filtrar: digitar filtra as opções pelo rótulo (sem diferenciar maiúsculas
// e acentos), as setas percorrem, o Enter escolhe. Feito para formulários usados pelo teclado:
// o Enter também confirma um campo já preenchido, e o onConfirm avisa quem usa para ir ao
// próximo campo. Não sabe de onde vêm os dados: recebe as opções prontas.
// options: [{ value, label, description? }]. value: o value escolhido (ou "").
// suggested: o valor veio como sugestão (outra cor) e o usuário ainda não mexeu.
// O ref chega como prop (React 19) e vai para o input, para quem usa poder dar o foco.
import { useEffect, useId, useState } from "react";
import { normalizeText } from "@/shared/utils/text";
import styles from "./Combobox.module.css";

export function Combobox({
  id,
  ref,
  options = [],
  value,
  onChange,
  onConfirm,
  placeholder,
  emptyMessage = "Nenhuma opção encontrada",
  openOnFocus = false,
  suggested = false,
  invalid = false,
  disabled = false,
}) {
  const listId = useId();
  // Texto digitado; null = não está digitando (o campo mostra o rótulo do escolhido)
  const [text, setText] = useState(null);
  const [isOpen, setIsOpen] = useState(false);
  // null = acompanha o escolhido (o Enter confirma o que está preenchido, mesmo que as opções
  // cheguem depois de abrir); número = o usuário navegou ou digitou
  const [highlighted, setHighlighted] = useState(null);

  const selected = options.find((option) => option.value === value) ?? null;
  const query = normalizeText(text);
  const visibleOptions = query
    ? options.filter((option) => normalizeText(option.label).includes(query))
    : options;
  const selectedIndex = Math.max(0, visibleOptions.findIndex((option) => option.value === value));
  const activeIndex = Math.min(highlighted ?? selectedIndex, visibleOptions.length - 1);
  const showList = isOpen && !disabled;

  // A opção destacada pelo teclado fica sempre visível
  useEffect(() => {
    if (showList) document.getElementById(`${listId}-${activeIndex}`)?.scrollIntoView({ block: "nearest" });
  }, [showList, listId, activeIndex]);

  // Abre destacando o escolhido (o Enter confirma o que já está preenchido)
  function open() {
    setHighlighted(null);
    setIsOpen(true);
  }

  // Fecha e volta o texto para o rótulo do escolhido
  function close() {
    setIsOpen(false);
    setText(null);
  }

  function choose(option) {
    onChange?.(option.value);
    close();
    onConfirm?.(option.value);
  }

  function handleFocus(event) {
    event.target.select();
    if (openOnFocus) open();
  }

  function handleChange(event) {
    setText(event.target.value);
    setHighlighted(0);
    setIsOpen(true);
  }

  function handleKeyDown(event) {
    if (event.key === "ArrowDown" || event.key === "ArrowUp") {
      event.preventDefault();
      if (!isOpen) {
        open();
        return;
      }
      if (visibleOptions.length === 0) return;
      const step = event.key === "ArrowDown" ? 1 : -1;
      setHighlighted((activeIndex + step + visibleOptions.length) % visibleOptions.length);
      return;
    }
    if (event.key === "Enter") {
      // Não envia o formulário: escolhe a destacada ou confirma o já preenchido
      event.preventDefault();
      if (isOpen && visibleOptions[activeIndex]) {
        choose(visibleOptions[activeIndex]);
      } else if (!isOpen && selected) {
        onConfirm?.(selected.value);
      }
      return;
    }
    // Com a lista aberta, o Esc só fecha a lista: não chega a um Modal em volta (que fecharia junto)
    if (event.key === "Escape" && isOpen) {
      event.stopPropagation();
      close();
      return;
    }
    if (event.key === "Tab") close();
  }

  return (
    <div className={styles.root}>
      <input
        id={id}
        ref={ref}
        type="text"
        role="combobox"
        aria-expanded={showList}
        aria-controls={showList ? listId : undefined}
        aria-activedescendant={showList && visibleOptions.length > 0 ? `${listId}-${activeIndex}` : undefined}
        aria-autocomplete="list"
        aria-invalid={invalid || undefined}
        autoComplete="off"
        disabled={disabled}
        placeholder={placeholder}
        className={[styles.input, suggested && styles.suggested, invalid && styles.invalid].filter(Boolean).join(" ")}
        value={text ?? selected?.label ?? ""}
        onFocus={handleFocus}
        onBlur={close}
        onChange={handleChange}
        onKeyDown={handleKeyDown}
      />

      {showList && (
        <ul id={listId} role="listbox" className={styles.list}>
          {visibleOptions.length === 0 && <li className={styles.empty}>{emptyMessage}</li>}
          {visibleOptions.map((option, index) => (
            <li
              key={option.value}
              id={`${listId}-${index}`}
              role="option"
              aria-selected={index === activeIndex}
              className={[styles.option, index === activeIndex && styles["option-active"]].filter(Boolean).join(" ")}
              onMouseEnter={() => setHighlighted(index)}
              // Sem isto, o clique tiraria o foco do campo (onBlur fecharia a lista antes do clique)
              onMouseDown={(event) => event.preventDefault()}
              onClick={() => choose(option)}
            >
              <span className={styles["option-label"]}>{option.label}</span>
              {option.description && <span className={styles["option-description"]}>{option.description}</span>}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
