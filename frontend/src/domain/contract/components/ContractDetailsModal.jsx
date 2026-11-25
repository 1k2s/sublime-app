import { useState } from "react";
import { ArrowLeft, FileText, Pencil, XCircle } from "lucide-react";
import { ContractBadge } from "./ContractBadge";
import { Button } from "@/shared/components/Button";
import { ListMessage } from "@/shared/components/ListMessage";
import { Modal } from "@/shared/components/Modal";
import { formatDate } from "@/shared/utils/date";
import { getContractStatus } from "../display/contractStatus";
import { getVersionBadge, getVersionLabel, hasAmendment } from "../display/contractVersions";
import { getVersionChanges } from "../display/versionChanges";
import { ContractVersionDetails } from "./ContractVersionDetails";
import styles from "./ContractDetailsModal.module.css";

const TABS = { CONTRACT: "contract", HISTORY: "history" };

const VERSION_MESSAGES = {
  loading: "Carregando o histórico...",
  error: "Não foi possível carregar o histórico",
  filtered: "Nenhuma versão encontrada",
  empty: "Nenhuma versão encontrada",
};

// Detalhe do contrato, em duas abas: "Contrato" (a versão atual, com Inativar e Editar) e
// "Histórico" (abre na versão mais nova, com a linha do tempo das versões no topo para abrir as
// outras e o que mudou em relação à anterior marcado em azul). O
// histórico é só para consultar: voltar a uma versão anterior é fazer um novo aditivo. Ver
// "Versionamento (aditivos)" em Contract, no domain-model.
// contract: a versão atual, já com patientName e beneficiaryName (withPatientNames). versions: as
// do /versions, com os nomes; a última é a original, que dá o número do contrato
export function ContractDetailsModal({
  contract,
  versions = [],
  isVersionsLoading = false,
  isVersionsError = false,
  isDeactivating = false,
  onDeactivate,
  onEdit,
  onClose,
}) {
  const [isConfirming, setIsConfirming] = useState(false);
  const [tab, setTab] = useState(TABS.CONTRACT);
  const [openedVersionId, setOpenedVersionId] = useState(null);
  // Cada troca de versão pela linha do tempo reinicia a animação da bolinha (duas animações iguais
  // com nomes diferentes, alternadas: o CSS só reinicia uma animação quando o nome muda)
  const [moveCount, setMoveCount] = useState(0);

  if (!contract) return null;

  const status = getContractStatus(contract);
  // O número do contrato é o da versão original: cada aditivo é uma versão nova (outro id), mas o
  // contrato é o mesmo
  const contractNumber = versions.at(-1)?.id ?? contract.id;
  // A aba Histórico abre na versão mais nova (o que o último aditivo mudou); a linha do tempo troca
  const openedVersion = versions.find((version) => version.id === openedVersionId) ?? versions[0] ?? null;

  function confirmDeactivate() {
    onDeactivate?.(contract);
    setIsConfirming(false);
  }

  function changeTab(nextTab) {
    setTab(nextTab);
    setOpenedVersionId(null);
    setMoveCount(0);
    setIsConfirming(false);
  }

  // O componente continua montado com o modal fechado: sem zerar, a aba e a confirmação
  // reapareceriam no próximo contrato
  function close() {
    changeTab(TABS.CONTRACT);
    onClose?.();
  }

  function renderFooter() {
    if (tab === TABS.HISTORY) {
      return (
        <div className={styles.footer}>
          <Button variant="outline" onClick={() => changeTab(TABS.CONTRACT)}>
            <ArrowLeft size={16} aria-hidden /> Voltar
          </Button>
          <Button variant="outline" onClick={close}>
            Fechar
          </Button>
        </div>
      );
    }
    // Durante a confirmação, o rodapé sai: as ações ficam na caixa de confirmação. Inativar e
    // Editar só no contrato ativo (vigente ou vencido): contrato não tem reativação
    if (isConfirming) return null;
    return (
      <div className={styles.footer}>
        <div>
          {contract.active && (
            <Button variant="danger" onClick={() => setIsConfirming(true)} disabled={isDeactivating}>
              <XCircle size={16} /> Inativar contrato
            </Button>
          )}
        </div>
        <div className={styles.actions}>
          <Button variant="outline" onClick={close}>
            Fechar
          </Button>
          {contract.active && (
            <Button onClick={() => onEdit?.(contract)} disabled={isDeactivating}>
              <Pencil size={16} aria-hidden /> Editar
            </Button>
          )}
        </div>
      </div>
    );
  }

  function renderHistory() {
    if (isVersionsLoading || isVersionsError || !openedVersion) {
      return (
        <ListMessage
          isLoading={isVersionsLoading}
          isError={isVersionsError}
          hasRecords={false}
          messages={VERSION_MESSAGES}
        />
      );
    }
    const badge = getVersionBadge(openedVersion);
    // Marca o que mudou em relação à versão anterior (a seguinte na lista, da mais nova para a
    // mais antiga); a original não tem anterior
    const previousVersion = versions[versions.indexOf(openedVersion) + 1] ?? null;
    const chronological = [...versions].reverse();
    const openedIndex = chronological.indexOf(openedVersion);

    function openVersion(versionId) {
      if (versionId === openedVersion.id) return;
      setOpenedVersionId(versionId);
      setMoveCount((count) => count + 1);
    }

    return (
      <div className={styles.version}>
        {/* Linha do tempo das versões, da original para a atual: mostra que é o histórico (não a
            aba Contrato) e abre cada versão com um clique. A bolinha azul (marker) desliza até a
            versão aberta, diminuindo no caminho */}
        <ol
          className={styles.timeline}
          aria-label="Versões do contrato"
          style={{ "--step-count": chronological.length, "--step-index": openedIndex }}
        >
          {chronological.map((version) => {
            const isOpen = version.id === openedVersion.id;
            const stepClass = [styles.step, !version.superseded && styles.stepCurrent, isOpen && styles.stepOpen]
              .filter(Boolean)
              .join(" ");
            return (
              <li key={version.id} className={stepClass}>
                <button
                  type="button"
                  className={styles.stepButton}
                  aria-current={isOpen ? "step" : undefined}
                  onClick={() => openVersion(version.id)}
                >
                  <span className={styles.stepDot} aria-hidden />
                  <strong>{getVersionLabel(version)}</strong>
                  <span>{version.superseded ? formatDate(version.createdAt) : "atual"}</span>
                </button>
              </li>
            );
          })}
          <li
            className={[styles.marker, moveCount > 0 && (moveCount % 2 ? styles.markerMoveA : styles.markerMoveB)]
              .filter(Boolean)
              .join(" ")}
            aria-hidden
          />
        </ol>
        {/* key: a cada versão o conteúdo é montado de novo, e entra com a animação */}
        <div key={openedVersion.id} className={styles.versionContent}>
          <div className={styles.versionTitle}>
            <strong>{getVersionLabel(openedVersion)}</strong>
            <ContractBadge variant={badge.badge}>{badge.label}</ContractBadge>
          </div>
          <ContractVersionDetails
            version={openedVersion}
            changes={getVersionChanges(openedVersion, previousVersion)}
            frame={openedVersion.superseded ? "dashed" : "solid"}
          />
        </div>
      </div>
    );
  }

  return (
    <Modal
      title={contract.patientName ?? "Contrato"}
      icon={FileText}
      size="medium"
      titleExtra={
        <span className={styles.badges}>
          <ContractBadge variant={status.badge}>{status.label}</ContractBadge>
          {hasAmendment(contract) && <ContractBadge>{getVersionLabel(contract)}</ContractBadge>}
        </span>
      }
      description={`Contrato nº ${contractNumber}`}
      footer={renderFooter()}
      onClose={close}
    >
      <div className={styles.content}>
        <div role="tablist" aria-label="Detalhe do contrato" className={styles.tabs}>
          <button
            type="button"
            role="tab"
            aria-selected={tab === TABS.CONTRACT}
            className={[styles.tab, tab === TABS.CONTRACT && styles.tabActive].filter(Boolean).join(" ")}
            onClick={() => changeTab(TABS.CONTRACT)}
          >
            Contrato
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={tab === TABS.HISTORY}
            className={[styles.tab, tab === TABS.HISTORY && styles.tabActive].filter(Boolean).join(" ")}
            onClick={() => changeTab(TABS.HISTORY)}
          >
            Histórico{versions.length > 0 ? ` (${versions.length})` : ""}
          </button>
          {/* A linha da aba ativa desliza de uma aba para a outra (as duas têm a mesma largura) */}
          <span
            className={[styles.tabIndicator, tab === TABS.HISTORY && styles.tabIndicatorSecond].filter(Boolean).join(" ")}
            aria-hidden
          />
        </div>

        {/* A mesma altura nas duas abas (o modal não muda de tamanho ao trocar): as listas rolam
            por dentro */}
        <div role="tabpanel" className={styles.panel}>
          {tab === TABS.CONTRACT ? <ContractVersionDetails version={contract} frame="solid" /> : renderHistory()}
        </div>

        {tab === TABS.CONTRACT && isConfirming && (
          <div className={styles.confirmBox}>
            <p className={styles.confirmText}>
              Inativar este contrato? Não tem volta: contrato não pode ser reativado. Use só para corrigir um
              contrato criado errado (ex: plano ou titular errado).
            </p>
            <div className={styles.actions}>
              <Button variant="outline" onClick={() => setIsConfirming(false)}>
                Cancelar
              </Button>
              <Button variant="danger" onClick={confirmDeactivate} disabled={isDeactivating}>
                Confirmar inativação
              </Button>
            </div>
          </div>
        )}
      </div>
    </Modal>
  );
}
