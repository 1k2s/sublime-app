// Barra de lançamento, toda pelo teclado: cada campo filtra o que se digita, as setas percorrem e
// o Enter escolhe e pula para o próximo campo; do status (o último), o Enter leva ao botão
// Lançar (destacado), e mais um Enter lança. A data fica fora da sequência: é Hoje, e o
// Retroativo (com liberação) se escolhe à parte. Os campos vêm do useLaunchDraft; os valores e o
// contrato saem da API.
import { useEffect, useRef } from "react";
import { parseISO, subDays } from "date-fns";
import { CalendarDays } from "lucide-react";
import { Button } from "@/shared/components/Button";
import { Combobox } from "@/shared/components/Combobox";
import { FormField } from "@/shared/components/FormField";
import { Input } from "@/shared/components/Input";
import { formatDate, toDateInputValue } from "@/shared/utils/date";
import { CONSULTATION_STATUS_OPTIONS } from "../display/consultationStatuses";
import { getSessionWarning } from "../display/sessionWarning";
import styles from "./LaunchBar.module.css";

// Ordem dos campos para o Enter (os que não aparecem são pulados)
const FIELD_ORDER = ["providerId", "patientId", "techniqueId", "contractId", "durationMinutes", "status"];

function SuggestionLabel({ children, isSuggested }) {
  return (
    <>
      {children}
      {isSuggested && <span className={styles.tag}>sugestão</span>}
    </>
  );
}

export function LaunchBar({
  draft,
  isAdmin,
  providerId,
  onProviderChange,
  today,
  providerOptions = [],
  patientOptions = [],
  techniqueOptions = [],
  retroactiveRemaining = 0,
  patientName,
  onLaunch,
  isLaunching = false,
  children,
}) {
  const providerRef = useRef(null);
  const patientRef = useRef(null);
  const techniqueRef = useRef(null);
  const contractRef = useRef(null);
  const durationRef = useRef(null);
  const statusRef = useRef(null);
  const launchRef = useRef(null);
  const canPickRetroactive = isAdmin || retroactiveRemaining > 0;
  const option = draft.chosenOption;
  const warning = getSessionWarning(option?.remainingSessions);
  const yesterday = today ? toDateInputValue(subDays(parseISO(today), 1)) : undefined;

  // Ao abrir e depois de cada lançamento: o foco vai para o primeiro campo vazio do começo
  useEffect(() => {
    (isAdmin && !providerId ? providerRef : patientRef).current?.focus();
    // Só no lançamento (resetCount) e ao abrir: não persegue o foco a cada mudança de campo
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [draft.resetCount]);

  // Enter num campo: vai para o próximo que estiver na tela (um campo que não aparece tem o ref nulo)
  function focusNext(current) {
    const refsByField = {
      providerId: providerRef,
      patientId: patientRef,
      techniqueId: techniqueRef,
      contractId: contractRef,
      durationMinutes: durationRef,
      status: statusRef,
    };
    const next = FIELD_ORDER.slice(FIELD_ORDER.indexOf(current) + 1).find((name) => refsByField[name].current);
    refsByField[next]?.current.focus();
  }

  function handleDurationKeyDown(event) {
    if (event.key === "Enter") {
      event.preventDefault();
      focusNext("durationMinutes");
    }
  }

  const durationOptions = (option?.durationOptions ?? []).map((minutes) => ({
    value: String(minutes),
    label: `${minutes} min`,
  }));
  const contractOptions = draft.launchOptions.map((launchOption) => ({
    value: String(launchOption.contractId),
    label: `${launchOption.holderName} · ${launchOption.planName}`,
    description: launchOption.patientIsHolder ? "titular" : "beneficiário",
  }));
  // Sem preço não há duração a escolher. Enquanto as opções carregam o campo fica habilitado: um
  // campo desabilitado não recebe o foco do Enter na técnica
  const isDurationDisabled = Boolean(option?.unavailableReason);

  // Dois contratos (titular e beneficiário): o campo Contrato aparece e recebe o foco
  useEffect(() => {
    if (draft.needsContractChoice && !draft.fields.contractId) contractRef.current?.focus();
    // Só quando a escolha passa a ser necessária
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [draft.needsContractChoice]);

  return (
    <section className={styles.bar} aria-label="Novo lançamento">
      <div className={styles.fields}>
        {isAdmin && (
          <div className={styles["field-provider"]}>
            <FormField label="Prestador" htmlFor="launch-provider" error={draft.errors.providerId}>
              <Combobox
                id="launch-provider"
                ref={providerRef}
                options={providerOptions}
                value={providerId}
                onChange={onProviderChange}
                onConfirm={() => focusNext("providerId")}
                placeholder="Digite o prestador"
                emptyMessage="Nenhum prestador encontrado"
                invalid={Boolean(draft.errors.providerId)}
              />
            </FormField>
          </div>
        )}

        <div className={styles["field-patient"]}>
          <FormField label="Paciente" htmlFor="launch-patient" error={draft.errors.patientId}>
            <Combobox
              id="launch-patient"
              ref={patientRef}
              options={patientOptions}
              value={draft.fields.patientId}
              onChange={draft.setPatientId}
              onConfirm={() => focusNext("patientId")}
              placeholder="Digite o nome"
              emptyMessage="Nenhum paciente encontrado"
              invalid={Boolean(draft.errors.patientId)}
            />
          </FormField>
        </div>

        <div className={styles["field-technique"]}>
          <FormField
            label={<SuggestionLabel isSuggested={draft.isSuggested("techniqueId")}>Técnica</SuggestionLabel>}
            htmlFor="launch-technique"
            error={draft.errors.techniqueId}
          >
            <Combobox
              id="launch-technique"
              ref={techniqueRef}
              options={techniqueOptions}
              value={draft.fields.techniqueId}
              onChange={draft.setTechniqueId}
              onConfirm={() => focusNext("techniqueId")}
              placeholder="Digite a técnica"
              emptyMessage="Nenhuma técnica encontrada"
              suggested={draft.isSuggested("techniqueId")}
              invalid={Boolean(draft.errors.techniqueId)}
            />
          </FormField>
        </div>

        {draft.needsContractChoice && (
          <div className={styles["field-contract"]}>
            <FormField label="Contrato" htmlFor="launch-contract" error={draft.errors.contractId}>
              <Combobox
                id="launch-contract"
                ref={contractRef}
                options={contractOptions}
                value={draft.fields.contractId}
                onChange={draft.setContractId}
                onConfirm={() => focusNext("contractId")}
                openOnFocus
                placeholder="De qual contrato?"
                invalid={Boolean(draft.errors.contractId)}
              />
            </FormField>
          </div>
        )}

        <div className={styles["field-duration"]}>
          <FormField
            label={<SuggestionLabel isSuggested={draft.isSuggested("durationMinutes")}>Duração</SuggestionLabel>}
            htmlFor="launch-duration"
            error={draft.errors.durationMinutes}
          >
            {option?.anyDurationAllowed ? (
              <Input
                id="launch-duration"
                ref={durationRef}
                type="number"
                min="1"
                inputMode="numeric"
                className={draft.isSuggested("durationMinutes") ? styles.suggested : undefined}
                value={draft.fields.durationMinutes}
                onChange={(event) => draft.setDurationMinutes(event.target.value)}
                onFocus={(event) => event.target.select()}
                onKeyDown={handleDurationKeyDown}
                invalid={Boolean(draft.errors.durationMinutes)}
              />
            ) : (
              <Combobox
                id="launch-duration"
                ref={durationRef}
                options={durationOptions}
                value={draft.fields.durationMinutes}
                onChange={draft.setDurationMinutes}
                onConfirm={() => focusNext("durationMinutes")}
                openOnFocus
                placeholder={draft.isLoadingOptions ? "..." : "min"}
                disabled={isDurationDisabled}
                suggested={draft.isSuggested("durationMinutes")}
                invalid={Boolean(draft.errors.durationMinutes)}
              />
            )}
          </FormField>
        </div>

        <div className={styles["field-status"]}>
          <FormField label="Status" htmlFor="launch-status" error={draft.errors.status}>
            <Combobox
              id="launch-status"
              ref={statusRef}
              options={CONSULTATION_STATUS_OPTIONS}
              value={draft.fields.status}
              onChange={draft.setStatus}
              onConfirm={() => launchRef.current?.focus()}
              openOnFocus
              invalid={Boolean(draft.errors.status)}
            />
          </FormField>
        </div>

        <div className={styles["field-date"]}>
          <FormField label="Data" htmlFor="launch-date" error={draft.errors.occurredAt}>
            {draft.isRetroactive ? (
              <div className={styles["date-picker"]}>
                <Input
                  id="launch-date"
                  type="date"
                  max={yesterday}
                  autoFocus
                  value={draft.retroactiveDate}
                  invalid={Boolean(draft.errors.occurredAt)}
                  onChange={(event) => draft.changeRetroactiveDate(event.target.value)}
                />
                <button type="button" className={styles["date-today"]} onClick={draft.chooseToday}>
                  Hoje
                </button>
              </div>
            ) : (
              <div className={styles.segment} role="group" aria-label="Data do atendimento">
                <button type="button" id="launch-date" className={styles["segment-on"]} aria-pressed="true">
                  Hoje
                </button>
                <button
                  type="button"
                  disabled={!canPickRetroactive}
                  title={canPickRetroactive ? undefined : "Lançamento retroativo precisa de liberação do administrador"}
                  onClick={draft.chooseRetroactive}
                >
                  <CalendarDays size={16} aria-hidden />
                  Retroativo{!isAdmin && retroactiveRemaining > 0 ? ` · ${retroactiveRemaining}` : ""}
                </button>
              </div>
            )}
          </FormField>
        </div>

        <div className={styles["field-action"]}>
          {/* O Enter no status traz o foco para cá (com destaque): mais um Enter lança */}
          <Button ref={launchRef} className={styles.launch} onClick={() => onLaunch()} loading={isLaunching}>
            Lançar
          </Button>
        </div>
      </div>

      {(warning || option?.unavailableReason || draft.retroactiveDate) && (
        <div className={styles.notices}>
          {warning && (
            <span className={styles[`notice-${warning.level}`]}>
              {patientName ? `${patientName}: ` : ""}
              {warning.text}
            </span>
          )}
          {option?.unavailableReason && <span className={styles["notice-danger"]}>{option.unavailableReason}</span>}
          {draft.isRetroactive && draft.retroactiveDate && (
            <span className={styles["notice-info"]}>Retroativo: {formatDate(draft.retroactiveDate)}</span>
          )}
        </div>
      )}

      {children}
    </section>
  );
}
