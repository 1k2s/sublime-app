import { useEffect, useId, useState } from "react";
import { Search } from "lucide-react";
import { Input } from "@/shared/ui/Input";
import { useTechniques } from "../api/usePricing";
import styles from "./TechniqueSearchInput.module.css";

function normalize(value) {
  return value.normalize("NFD").replace(/\p{Diacritic}/gu, "").toLowerCase().trim();
}

export function TechniqueSearchInput({ id, ref, onSelect, invalid = false, autoFocus = false }) {
  const listId = useId();
  const [text, setText] = useState("");
  const [open, setOpen] = useState(autoFocus);
  const [highlighted, setHighlighted] = useState(0);
  const { data: techniques = [], isLoading, isError } = useTechniques();

  const query = normalize(text);
  const matches = techniques.filter((technique) => normalize(technique.name).includes(query));
  const showResults = open && matches.length > 0;
  const activeIndex = Math.min(highlighted, matches.length - 1);

  useEffect(() => {
    if (showResults) document.getElementById(`${listId}-${activeIndex}`)?.scrollIntoView({ block: "nearest" });
  }, [showResults, listId, activeIndex]);

  function handleChange(event) {
    setText(event.target.value);
    setHighlighted(0);
    setOpen(true);
  }

  function handleKeyDown(event) {
    if (event.key === "ArrowDown") {
      event.preventDefault();
      if (!open) setOpen(true);
      else if (matches.length > 0) setHighlighted((activeIndex + 1) % matches.length);
    } else if (event.key === "ArrowUp" && showResults) {
      event.preventDefault();
      setHighlighted((activeIndex - 1 + matches.length) % matches.length);
    } else if (event.key === "Enter") {
      event.preventDefault();
      if (showResults) onSelect(matches[activeIndex]);
      else setOpen(true);
    } else if (event.key === "Escape" && open) {
      event.preventDefault();
      setOpen(false);
    }
  }

  function renderResults() {
    if (isLoading) return <p className={styles["search-message"]}>Carregando...</p>;
    if (isError) return <p className={styles["search-message"]}>Não foi possível carregar as técnicas</p>;
    if (matches.length === 0) return <p className={styles["search-message"]}>Nenhuma técnica encontrada</p>;

    return (
      <ul id={listId} role="listbox" className={styles["search-list"]}>
        {matches.map((technique, index) => (
          <li
            key={technique.id}
            id={`${listId}-${index}`}
            role="option"
            aria-selected={index === activeIndex}
            className={`${styles["search-option"]} ${index === activeIndex ? styles["search-option-active"] : ""}`}
            onMouseEnter={() => setHighlighted(index)}
            onMouseDown={(event) => event.preventDefault()}
            onClick={() => onSelect(technique)}
          >
            {technique.name}
          </li>
        ))}
      </ul>
    );
  }

  return (
    <div className={styles["search-wrapper"]}>
      <div className={styles["search-field"]}>
        <Search className={styles["search-icon"]} size={18} aria-hidden />
        <Input
          ref={ref}
          id={id}
          type="search"
          role="combobox"
          aria-expanded={showResults}
          aria-controls={showResults ? listId : undefined}
          aria-activedescendant={showResults ? `${listId}-${activeIndex}` : undefined}
          autoComplete="off"
          autoFocus={autoFocus}
          placeholder="Digite a técnica"
          className={styles["search-input"]}
          invalid={invalid}
          value={text}
          onChange={handleChange}
          onKeyDown={handleKeyDown}
          onFocus={() => setOpen(true)}
          onBlur={() => setOpen(false)}
        />
      </div>
      {open && <div className={styles["search-results"]}>{renderResults()}</div>}
    </div>
  );
}
