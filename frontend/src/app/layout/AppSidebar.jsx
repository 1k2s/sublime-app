// Menu lateral: a pílula clara marca a tela aberta; a translúcida acompanha o mouse e o Tab.
// Abre com o mouse em cima ou o foco do teclado dentro
import { useEffect, useRef, useState } from "react";
import { NavLink, matchPath, useLocation } from "react-router-dom";
import { LogOut } from "lucide-react";
import { useAuth } from "@/domain/auth/context/useAuth";
import { getRole } from "@/domain/auth/roles";
import { BackgroundShapes } from "./BackgroundShapes";
import { navigationItems } from "./navigationItems";
import styles from "./AppSidebar.module.css";

// Atrasos do hover: não abre com o mouse só de passagem, nem fecha num escorregão
const OPEN_DELAY_MS = 60;
const CLOSE_DELAY_MS = 150;

export function AppSidebar() {
  const { user, signOut } = useAuth();
  const { pathname } = useLocation();
  const [expanded, setExpanded] = useState(false);
  const hoverTimer = useRef(null);
  const isHovered = useRef(false);
  // Ao voltar ao menu, a pílula aparece direto no item (instant), sem deslizar do anterior
  const [pointer, setPointer] = useState({ index: null, visible: false, instant: true });

  useEffect(() => () => clearTimeout(hoverTimer.current), []);

  const visibleItems = navigationItems.filter((item) => item.roles.includes(user.role));
  // Pela URL: /patients/new marca Pacientes; rota fora do menu dá -1
  const activeIndex = visibleItems.findIndex(
    (item) => !item.disabled && matchPath({ path: item.path, end: false }, pathname),
  );
  const pillIndex = pointer.index ?? Math.max(activeIndex, 0);

  function expandAfter(delay) {
    clearTimeout(hoverTimer.current);
    hoverTimer.current = setTimeout(() => setExpanded(true), delay);
  }

  function collapseAfter(delay) {
    clearTimeout(hoverTimer.current);
    hoverTimer.current = setTimeout(() => setExpanded(false), delay);
  }

  // Só o foco do teclado abre (o clique também foca o link, e o mouse já abriu)
  function handleFocus(event) {
    if (event.target.matches(":focus-visible")) {
      expandAfter(0);
    }
  }

  // Tab para fora do menu. Com o mouse em cima, quem fecha é o mouse: a tela aberta pode levar o
  // foco para um campo (ex: o Lançamento) e o menu não deve fechar por isso
  function handleBlur(event) {
    if (!isHovered.current && !event.currentTarget.contains(event.relatedTarget)) {
      collapseAfter(0);
    }
  }

  function handleMouseEnter() {
    isHovered.current = true;
    expandAfter(OPEN_DELAY_MS);
  }

  function handleMouseLeave() {
    isHovered.current = false;
    collapseAfter(CLOSE_DELAY_MS);
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

  return (
    <nav
      aria-label="Menu principal"
      className={`${styles.sidebar} ${expanded ? "" : styles.collapsed}`}
      onMouseEnter={handleMouseEnter}
      onMouseLeave={handleMouseLeave}
      onFocus={handleFocus}
      onBlur={handleBlur}
    >
      <div className={styles.header}>
        <img className={styles.headerLogo} src="/sublime-logo-symbol.png" alt="" />
        <span className={styles.brandName}>Sublime</span>
      </div>

      <div
        className={styles.items}
        style={{ "--active": activeIndex, "--pointed": pillIndex }}
        onMouseLeave={hidePointer}
        onBlur={handleListBlur}
      >
        {activeIndex >= 0 && <span className={styles.activePill} aria-hidden="true" />}
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
  );
}
