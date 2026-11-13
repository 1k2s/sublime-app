// Menu lateral: marca, itens de navegação e usuário logado
import { NavLink } from "react-router-dom";
import { Menu, MenuItem, Sidebar, sidebarClasses } from "react-pro-sidebar";
import { LogOut, User } from "lucide-react";
import { useAuth } from "@/domain/auth/context/useAuth";
import { getRole } from "@/domain/auth/roles";
import { navigationItems } from "./navigationItems";
import styles from "./AppSidebar.module.css";

// O menu se separa do conteúdo pela sombra, como os cards (sem linha na borda)
const sidebarRootStyles = {
  borderRight: "none",
  boxShadow: "1px 0 12px rgb(16 24 40 / 0.05)",
  zIndex: 1,
  [`.${sidebarClasses.container}`]: {
    display: "flex",
    flexDirection: "column",
  },
};

// O item da tela aberta se destaca: fundo verde-claro, texto e ícone na cor do sistema e a barra
// à esquerda
const menuItemStyles = {
  button: ({ disabled }) => ({
    height: 46,
    margin: "3px 12px",
    padding: "0 14px",
    borderLeft: "3px solid transparent",
    borderRadius: 10,
    fontSize: 15,
    color: disabled ? "var(--color-muted)" : "var(--color-text)",
    opacity: disabled ? 0.6 : 1,
    transition: "background-color 0.15s ease, color 0.15s ease",
    "&:hover": { backgroundColor: "var(--color-primary-soft)" },
    "&.active": {
      backgroundColor: "var(--color-primary-soft)",
      borderLeftColor: "var(--color-primary)",
      color: "var(--color-primary)",
      fontWeight: 600,
    },
  }),
  icon: { minWidth: 26, width: 26, marginRight: 10 },
};

export function AppSidebar() {
  const { user, signOut } = useAuth();
  const visibleItems = navigationItems.filter((item) => item.roles.includes(user.role));

  return (
    <Sidebar width="240px" backgroundColor="#ffffff" rootStyles={sidebarRootStyles}>
      <div className={styles.brand}>
        <span className={styles.brandName}>Sublime</span>
        <span className={styles.brandTagline}>Fisioterapia Especializada</span>
      </div>

      <Menu menuItemStyles={menuItemStyles}>
        {visibleItems.map(({ label, path, icon: Icon, disabled }) => (
          <MenuItem
            key={path}
            icon={<Icon size={20} />}
            disabled={disabled}
            component={disabled ? undefined : <NavLink to={path} />}
          >
            {label}
          </MenuItem>
        ))}
      </Menu>
      <div className={styles.user}>
        <span className={styles.avatar}>
          <User size={18} />
        </span>
        <div className={styles.userInfo}>
          <span className={styles.userName}>{user.email}</span>
          <span className={styles.userRole}>{getRole(user.role)?.label}</span>
        </div>
        <button type="button" className={styles.logout} aria-label="Sair" title="Sair" onClick={signOut}>
          <LogOut size={18} />
        </button>
      </div>
    </Sidebar>
  );
}
