// O lançamento em montagem: os campos da barra, quais deles são sugestão (do paciente recente),
// as opções do lançamento (do backend) e o pedido no formato do ConsultationRequestDTO.
// O contrato, as durações válidas e a sugerida vêm da API (launch-options): aqui só se escolhe
// entre o que ela devolveu. Ver "Contrato do atendimento" em Consultation, no domain-model.
import { useState } from "react";
import { z } from "zod";
import { useLaunchOptions } from "../api/useConsultations";
import { DEFAULT_CONSULTATION_STATUS } from "../display/consultationStatuses";
import { adminLaunchSchema, launchSchema } from "../schemas/launchSchema";

const EMPTY_FIELDS = {
  patientId: "",
  techniqueId: "",
  contractId: "",
  durationMinutes: "",
  status: DEFAULT_CONSULTATION_STATUS,
};

// isAdmin: o ADMIN lança por um prestador (providerId, do useLaunchProvider) e em qualquer data.
// today: o "hoje" da clínica, do resumo do dia (backend). canLaunchOnOtherDay: ADMIN, ou
// PROVIDER com liberação ativa
export function useLaunchDraft({ isAdmin, providerId, today, canLaunchOnOtherDay }) {
  const [fields, setFields] = useState(EMPTY_FIELDS);
  const [suggestedFields, setSuggestedFields] = useState([]);
  // Data: "Hoje" ou "Retroativo" (só com liberação, ou para o ADMIN). No retroativo, a data
  // precisa ser escolhida: sem ela, o lançamento não sai (nunca cai em hoje sem avisar)
  const [isRetroactiveChosen, setIsRetroactiveChosen] = useState(false);
  const [retroactiveDate, setRetroactiveDate] = useState("");
  const [errors, setErrors] = useState({});
  // Muda a cada lançamento: a barra volta o foco para o paciente
  const [resetCount, setResetCount] = useState(0);

  // Sem liberação (ex: acabou durante o uso), volta para hoje
  const isRetroactive = isRetroactiveChosen && canLaunchOnOtherDay;
  const occurredAt = isRetroactive ? retroactiveDate : today;

  const optionsQuery = useLaunchOptions({ patientId: fields.patientId, techniqueId: fields.techniqueId, occurredAt });
  const launchOptions = optionsQuery.data ?? [];
  const needsContractChoice = launchOptions.length > 1;
  const chosenOption =
    launchOptions.length === 1
      ? launchOptions[0]
      : (launchOptions.find((option) => String(option.contractId) === fields.contractId) ?? null);

  // A duração escolhida vale se está entre as válidas da opção; senão, vale a sugerida pelo
  // backend (a travada do contrato, a única, ou 60 no Pilates em Grupo)
  const isDurationValid =
    fields.durationMinutes !== "" &&
    (!chosenOption ||
      chosenOption.anyDurationAllowed ||
      chosenOption.durationOptions.includes(Number(fields.durationMinutes)));
  const durationMinutes = isDurationValid
    ? fields.durationMinutes
    : chosenOption?.suggestedDurationMinutes != null
      ? String(chosenOption.suggestedDurationMinutes)
      : "";

  function isSuggested(field) {
    if (field === "durationMinutes" && !isDurationValid) return false;
    return suggestedFields.includes(field);
  }

  function clearError(...names) {
    setErrors((current) => {
      const next = { ...current };
      names.forEach((name) => delete next[name]);
      return next;
    });
  }

  // Outro paciente: o resto do lançamento era do anterior
  function setPatientId(value) {
    setFields({ ...EMPTY_FIELDS, patientId: value });
    setSuggestedFields([]);
    clearError("patientId", "techniqueId", "durationMinutes", "contractId");
  }

  function setTechniqueId(value) {
    setFields((current) => ({ ...current, techniqueId: value, contractId: "", durationMinutes: "" }));
    setSuggestedFields((current) => current.filter((field) => field !== "techniqueId" && field !== "durationMinutes"));
    clearError("techniqueId", "contractId", "durationMinutes");
  }

  function setContractId(value) {
    setFields((current) => ({ ...current, contractId: value }));
    clearError("contractId");
  }

  function setDurationMinutes(value) {
    setFields((current) => ({ ...current, durationMinutes: value }));
    setSuggestedFields((current) => current.filter((field) => field !== "durationMinutes"));
    clearError("durationMinutes");
  }

  function setStatus(value) {
    setFields((current) => ({ ...current, status: value }));
    clearError("status");
  }

  function chooseToday() {
    setIsRetroactiveChosen(false);
    setRetroactiveDate("");
    clearError("occurredAt");
  }

  function chooseRetroactive() {
    setIsRetroactiveChosen(true);
    clearError("occurredAt");
  }

  function changeRetroactiveDate(value) {
    setRetroactiveDate(value);
    clearError("occurredAt");
  }

  // Paciente recente: a técnica e a duração do último atendimento dele vêm como sugestão; o
  // status volta para Atendido (sugerir uma falta levaria a erro)
  function applyRecent(recent) {
    setFields({
      ...EMPTY_FIELDS,
      patientId: String(recent.patientId),
      techniqueId: String(recent.techniqueId),
      durationMinutes: String(recent.durationMinutes),
    });
    setSuggestedFields(["techniqueId", "durationMinutes"]);
    setErrors({});
  }

  // Erros por campo da API (errors do ProblemDetail): os nomes são os mesmos dos campos
  function applyApiErrors(apiErrors) {
    if (apiErrors) setErrors(apiErrors);
  }

  // O pedido no formato do ConsultationRequestDTO, ou null depois de marcar o erro
  function buildRequest() {
    const values = { ...fields, durationMinutes, providerId };
    const result = (isAdmin ? adminLaunchSchema : launchSchema).safeParse(values);
    if (!result.success) {
      const { fieldErrors } = z.flattenError(result.error);
      setErrors(Object.fromEntries(Object.entries(fieldErrors).map(([field, messages]) => [field, messages[0]])));
      return null;
    }
    if (isRetroactive && !retroactiveDate) {
      setErrors({ occurredAt: "Escolha a data do retroativo ou volte para Hoje." });
      return null;
    }
    if (optionsQuery.isLoading || !today) {
      setErrors({ durationMinutes: "Aguarde: buscando o contrato do paciente." });
      return null;
    }
    if (needsContractChoice && !chosenOption) {
      setErrors({ contractId: "Escolha de qual contrato sai a sessão." });
      return null;
    }
    if (chosenOption?.unavailableReason) {
      setErrors({ techniqueId: chosenOption.unavailableReason });
      return null;
    }
    return {
      patientId: Number(fields.patientId),
      contractId: chosenOption?.contractId ?? null,
      providerId: isAdmin ? Number(providerId) : null,
      techniqueId: Number(fields.techniqueId),
      durationMinutes: Number(durationMinutes),
      occurredAt,
      status: fields.status,
    };
  }

  // Depois de lançar: limpa o lançamento e a data volta para Hoje (o prestador do ADMIN fica)
  function reset() {
    setFields(EMPTY_FIELDS);
    setSuggestedFields([]);
    setIsRetroactiveChosen(false);
    setRetroactiveDate("");
    setErrors({});
    setResetCount((count) => count + 1);
  }

  return {
    fields: { ...fields, durationMinutes },
    occurredAt,
    isRetroactive,
    retroactiveDate,
    errors,
    resetCount,
    launchOptions,
    isLoadingOptions: optionsQuery.isLoading,
    needsContractChoice,
    chosenOption,
    isSuggested,
    setPatientId,
    setTechniqueId,
    setContractId,
    setDurationMinutes,
    setStatus,
    chooseToday,
    chooseRetroactive,
    changeRetroactiveDate,
    applyRecent,
    applyApiErrors,
    buildRequest,
    reset,
  };
}
