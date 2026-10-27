/**
 * ======================================================================================
 * COMPONENTE REUTILIZÁVEL: PageHeader
 * ======================================================================================
 *
 * Cabeçalho padrão de páginas do sistema:
 * - Título principal com tipografia destacada e subtítulo descritivo.
 * - Suporte opcional a botão de retorno (`onBack`), exibindo a seta para a esquerda (`ArrowLeft`).
 * - Slot de ações à direita (`action`), utilizado para botões de criação primários como "+ Novo paciente".
 */

import { ArrowLeft } from "lucide-react";
import styles from "./PageHeader.module.css";

/**
 * @param {Object} props
 * @param {string} props.title - Título da página
 * @param {string} [props.description] - Subtítulo explicativo
 * @param {function(): void} [props.onBack] - Se fornecido, exibe o botão de voltar à esquerda do título
 * @param {import("react").ReactNode} [props.action] - Elemento de ação renderizado à direita do cabeçalho
 * @param {string} [props.className] - Classes CSS adicionais
 */
export function PageHeader({ title, description, onBack, action, className }) {
  return (
    <div className={[styles.header, className].filter(Boolean).join(" ")}>
      <div className={styles.titleArea}>
        {/* Botão de retorno estilizado */}
        {onBack && (
          <button
            type="button"
            className={styles.backButton}
            onClick={onBack}
            aria-label="Voltar"
            title="Voltar"
          >
            <ArrowLeft size={22} />
          </button>
        )}
        {/* Título e descrição da tela */}
        <div>
          <h1 className={styles.title}>{title}</h1>
          {description && <p className={styles.description}>{description}</p>}
        </div>
      </div>

      {/* Ações contextuais (ex.: botão "+ Novo paciente") */}
      {action && <div className={styles.actionArea}>{action}</div>}
    </div>
  );
}
