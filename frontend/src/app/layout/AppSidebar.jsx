// Menu lateral: a pílula clara marca a tela aberta; a translúcida acompanha o mouse e o Tab.
// Abre e recolhe pelo botão na borda (entre o logo e o primeiro item), e lembra a escolha no
// navegador
import { useState } from "react";
import { NavLink, matchPath, useLocation } from "react-router-dom";
import { ChevronLeft, ChevronRight, LogOut } from "lucide-react";
import { useAuth } from "@/domain/auth/context/useAuth";
import { getRole } from "@/domain/auth/roles";
import { BackgroundShapes } from "./BackgroundShapes";
import { navigationItems } from "./navigationItems";
import styles from "./AppSidebar.module.css";

// Menu aberto ou recolhido, guardado no navegador (só conveniência: sem o dado, abre recolhido).
// O acesso pode falhar (janela anônima, armazenamento bloqueado): por isso o try
const EXPANDED_KEY = "sublime.sidebarExpanded";

function readExpanded() {
  try {
    return localStorage.getItem(EXPANDED_KEY) === "true";
  } catch {
    return false;
  }
}

function saveExpanded(expanded) {
  try {
    localStorage.setItem(EXPANDED_KEY, String(expanded));
  } catch {
    // sem armazenamento: o menu só não lembra a escolha
  }
}

export function AppSidebar() {
  const { user, signOut } = useAuth();
  const { pathname } = useLocation();
  const [expanded, setExpanded] = useState(readExpanded);
  // Ao voltar ao menu, a pílula aparece direto no item (instant), sem deslizar do anterior
  const [pointer, setPointer] = useState({ index: null, visible: false, instant: true });

  const visibleItems = navigationItems.filter((item) => item.roles.includes(user.role));
  // Pela URL: /patients/new marca Pacientes; rota fora do menu dá -1
  const activeIndex = visibleItems.findIndex(
    (item) => !item.disabled && matchPath({ path: item.path, end: false }, pathname),
  );
  const pillIndex = pointer.index ?? Math.max(activeIndex, 0);

  // Cada troca de tela reinicia o efeito de bolha da pílula clara (duas animações iguais com nomes
  // diferentes, alternadas: o CSS só reinicia uma animação quando o nome muda). Estado derivado,
  // atualizado no render quando a tela muda
  const [lastActiveIndex, setLastActiveIndex] = useState(activeIndex);
  const [bubbleCount, setBubbleCount] = useState(0);
  if (activeIndex !== lastActiveIndex) {
    setLastActiveIndex(activeIndex);
    setBubbleCount((count) => count + 1);
  }
  const bubbleClass = bubbleCount === 0 ? "" : bubbleCount % 2 ? styles.bubbleA : styles.bubbleB;

  function toggleExpanded() {
    setExpanded((current) => {
      saveExpanded(!current);
      return !current;
    });
  }

  function pointAt(index) {
    setPointer((current) => ({ index, visible: true, instant: !current.visible }));
  }

  function hidePointer() {
    setPointer((current) => ({ ...current, visible: false }));
  }

  // Tab para fora da lista
  function handleListBlur(event) {
    if (!event.currentTarget.contains(event.relatedTarget)) {
      hidePointer();
    }
  }

  // A moldura deixa o botão sobre a borda: o nav corta o que passa da largura (overflow)
  return (
    <div className={`${styles.frame} ${expanded ? "" : styles.collapsed}`}>
      <nav aria-label="Menu principal" id="main-menu" className={styles.sidebar}>
        <div className={styles.header}>
          {/* O logo também abre e recolhe o menu, como o botão da borda. Fora do Tab (tabIndex -1):
              pelo teclado, o caminho é o botão da borda, e o Tab não para duas vezes na mesma ação */}
          <button
            type="button"
            tabIndex={-1}
            className={styles.headerLogo}
            aria-controls="main-menu"
            aria-expanded={expanded}
            aria-label={expanded ? "Recolher menu" : "Expandir menu"}
            title={expanded ? "Recolher menu" : "Expandir menu"}
            onClick={toggleExpanded}
          />
          <span className={styles.brandName}>Sublime</span>
        </div>

        {/* A borda com o painel e a pílula clara numa camada só, com uma sombra só: a sombra segue o
            contorno inteiro (borda, curvas e pílula), sem emenda */}
        <div className={styles.edgeLayer} style={{ "--active": activeIndex }} aria-hidden="true">
          {activeIndex >= 0 && <span className={`${styles.activePill} ${bubbleClass}`} />}
        </div>

        <div
          className={styles.items}
          style={{ "--pointed": pillIndex }}
          onMouseLeave={hidePointer}
          onBlur={handleListBlur}
        >
          <span
            className={`${styles.hoverPill} ${pointer.instant ? styles.instant : ""} ${
              pointer.visible && pointer.index !== activeIndex ? styles.visible : ""
            }`}
            aria-hidden="true"
          />

          <ul className={styles.list}>
            {visibleItems.map(({ label, path, icon: Icon, disabled }, index) => {
              const content = (
                <>
                  <span className={styles.icon}>
                    <Icon size={20} aria-hidden="true" />
                  </span>
                  <span className={styles.label}>{label}</span>
                </>
              );

              return (
                <li key={path}>
                  {disabled ? (
                    <span className={`${styles.item} ${styles.disabled}`} aria-disabled="true">
                      {content}
                    </span>
                  ) : (
                    <NavLink
                      to={path}
                      className={`${styles.item} ${index === activeIndex ? styles.active : ""}`}
                      onMouseEnter={() => pointAt(index)}
                      onFocus={() => pointAt(index)}
                    >
                      {content}
                    </NavLink>
                  )}
                </li>
              );
            })}
          </ul>
        </div>

        {/* Fica no recolhido para esmaecer, em vez de sumir de uma vez */}
        <div className={styles.logoArea} aria-hidden="true">
          <img className={styles.backgroundLogo} src="/sublime-logo-symbol.png" alt="" />
        </div>

        <BackgroundShapes placement="sidebar" className={styles.shapes} />

        <div className={styles.user}>
          <button type="button" className={styles.logout} aria-label="Sair" onClick={signOut}>
            <LogOut size={20} aria-hidden="true" />
          </button>
          <div className={styles.userInfo}>
            <span className={styles.userEmail}>{user.email}</span>
            <span className={styles.userRole}>{getRole(user.role)?.label}</span>
          </div>
        </div>
      </nav>

      {/* Recolhido, o nome não aparece: o balão mostra o nome do item com o mouse em cima ou o foco.
          Só visual (aria-hidden): o leitor de tela já lê o nome do item, escondido só da vista */}
      {!expanded && pointer.visible && pointer.index != null && (
        <span
          className={`${styles.tooltip} ${pointer.instant ? styles.tooltipInstant : ""}`}
          style={{ "--pointed": pointer.index }}
          aria-hidden="true"
        >
          {visibleItems[pointer.index]?.label}
        </span>
      )}

      <button
        type="button"
        className={styles.toggle}
        aria-controls="main-menu"
        aria-expanded={expanded}
        aria-label={expanded ? "Recolher menu" : "Expandir menu"}
        title={expanded ? "Recolher menu" : "Expandir menu"}
        onClick={toggleExpanded}
      >
        {expanded ? <ChevronLeft size={18} aria-hidden="true" /> : <ChevronRight size={18} aria-hidden="true" />}
      </button>
    </div>
  );
}
