// Busca de paciente por nome ou CPF. Setas navegam pelos resultados e Enter entrega o destacado em onSelect
import { useEffect, useId, useState } from "react";
import { Search } from "lucide-react";
import { Input } from "@/shared/ui/Input";
import { maskCpf } from "@/shared/utils/masks";
import { usePatients } from "../api/usePatients";
import styles from "./PatientSearchInput.module.css";

const MIN_SEARCH_LENGTH = 2;
const DEBOUNCE_MS = 300;

export function PatientSearchInput({ id, onSelect, invalid = false, autoFocus = false }) {
  const listId = useId();
  const [text, setText] = useState("");
  const [search, setSearch] = useState("");
  const [highlighted, setHighlighted] = useState(0);

  useEffect(() => {
    const timeout = setTimeout(() => setSearch(text.trim()), DEBOUNCE_MS);
    return () => clearTimeout(timeout);
  }, [text]);

  const enabled = search.length >= MIN_SEARCH_LENGTH;
  const { data: patients = [], isLoading, isError } = usePatients({ search, status: "ACTIVE" }, { enabled });
  const showResults = enabled && patients.length > 0;
  const activeIndex = Math.min(highlighted, patients.length - 1);

  useEffect(() => {
    if (showResults) document.getElementById(`${listId}-${activeIndex}`)?.scrollIntoView({ block: "nearest" });
  }, [showResults, listId, activeIndex]);

  function handleChange(event) {
    setText(event.target.value);
    setHighlighted(0);
  }

  function handleKeyDown(event) {
    if (event.key === "ArrowDown" && showResults) {
      event.preventDefault();
      setHighlighted((activeIndex + 1) % patients.length);
    } else if (event.key === "ArrowUp" && showResults) {
      event.preventDefault();
      setHighlighted((activeIndex - 1 + patients.length) % patients.length);
    } else if (event.key === "Enter" && text.trim().length >= MIN_SEARCH_LENGTH) {
      // Não envia o formulário: com resultados, escolhe o destacado; com a busca pendente, espera
      event.preventDefault();
      const isUpToDate = text.trim() === search && !isLoading;
      if (isUpToDate && showResults) onSelect(patients[activeIndex]);
    }
  }

  function renderResults() {
    if (isLoading) return <p className={styles["search-message"]}>Buscando...</p>;
    if (isError) return <p className={styles["search-message"]}>Não foi possível buscar os pacientes</p>;
    if (patients.length === 0) return <p className={styles["search-message"]}>Nenhum paciente encontrado</p>;

    return (
      <ul id={listId} role="listbox" className={styles["search-list"]}>
        {patients.map((patient, index) => (
          <li
            key={patient.id}
            id={`${listId}-${index}`}
            role="option"
            aria-selected={index === activeIndex}
            className={`${styles["search-option"]} ${index === activeIndex ? styles["search-option-active"] : ""}`}
            onMouseEnter={() => setHighlighted(index)}
            onClick={() => onSelect(patient)}
          >
            <span className={styles["search-option-name"]}>{patient.name}</span>
            <span className={styles["search-option-cpf"]}>{maskCpf(patient.cpf)}</span>
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
          id={id}
          type="search"
          role="combobox"
          aria-expanded={showResults}
          aria-controls={showResults ? listId : undefined}
          aria-activedescendant={showResults ? `${listId}-${activeIndex}` : undefined}
          autoComplete="off"
          autoFocus={autoFocus}
          placeholder="Digite o nome ou CPF"
          className={styles["search-input"]}
          invalid={invalid}
          value={text}
          onChange={handleChange}
          onKeyDown={handleKeyDown}
        />
      </div>
      {enabled && <div className={styles["search-results"]}>{renderResults()}</div>}
    </div>
  );
}
