// Estado do carrinho de lançamentos: o item em edição (paciente, técnica e chegada) e os itens já adicionados
import { useState } from "react";
import { z } from "zod";
import { toDateTimeInputValue } from "@/shared/utils/date";
import { consultationSchema } from "../schemas/consultationSchema";

function now() {
  return toDateTimeInputValue(new Date());
}

// Mesmo paciente pode entrar mais de uma vez, desde que com técnicas diferentes
function itemKey(patientId, techniqueId) {
  return `${patientId}-${techniqueId}`;
}

export function useConsultationCart() {
  const [patient, setPatient] = useState(null);
  const [technique, setTechnique] = useState(null);
  const [arrivalTime, setArrivalTimeValue] = useState(now);
  const [errors, setErrors] = useState({});
  const [items, setItems] = useState([]);

  function selectPatient(selectedPatient) {
    setPatient(selectedPatient);
    setErrors((current) => ({ ...current, patientId: undefined }));
  }

  function selectTechnique(selectedTechnique) {
    setTechnique(selectedTechnique);
    setErrors((current) => ({ ...current, techniqueId: undefined }));
  }

  function setArrivalTime(value) {
    setArrivalTimeValue(value);
    setErrors((current) => ({ ...current, arrivalTime: undefined }));
  }

  // Valida o item em edição. Devolve o payload da API, ou null depois de marcar os erros
  function validateDraft() {
    const result = consultationSchema.safeParse({
      patientId: patient?.id,
      techniqueId: technique?.id,
      arrivalTime,
    });

    if (!result.success) {
      const { fieldErrors } = z.flattenError(result.error);
      setErrors(Object.fromEntries(Object.entries(fieldErrors).map(([field, messages]) => [field, messages[0]])));
      return null;
    }

    if (items.some((item) => item.key === itemKey(patient.id, technique.id))) {
      setErrors({ techniqueId: "Este paciente já está no carrinho com essa técnica" });
      return null;
    }

    setErrors({});
    return result.data;
  }

  // Adiciona e limpa o item em edição para o próximo paciente
  function addItem(item) {
    setItems((current) => [...current, { ...item, key: itemKey(item.patient.id, item.technique.id) }]);
    setPatient(null);
    setTechnique(null);
    setArrivalTimeValue(now());
  }

  function removeItem(key) {
    setItems((current) => current.filter((item) => item.key !== key));
  }

  function clear() {
    setItems([]);
  }

  return {
    patient,
    technique,
    arrivalTime,
    errors,
    items,
    selectPatient,
    selectTechnique,
    setArrivalTime,
    validateDraft,
    addItem,
    removeItem,
    clear,
  };
}
