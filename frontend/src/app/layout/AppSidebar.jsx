// Menu lateral: marca, itens de navegação e usuário logado
import { NavLink } from "react-router-dom";
import { Menu, MenuItem, Sidebar, sidebarClasses } from "react-pro-sidebar";
import { LogOut, User } from "lucide-react";
import { useAuth } from "@/domain/auth/context/useAuth";
import { getRole } from "@/domain/auth/roles";
import { navigationItems } from "./navigationItems";
import styles from "./AppSidebar.module.css";

// O menu fica sobre a moldura escura do layout, sem fundo nem sombra próprios
const sidebarRootStyles = {
  borderRight: "none",
  [`.${sidebarClasses.container}`]: {
    display: "flex",
    flexDirection: "column",
  },
};

// O item da tela aberta se destaca: fundo um tom acima da moldura, borda sutil, texto branco e um brilho azul
// (a cor de destaque, #216fb0) no topo
const menuItemStyles = {
  button: ({ disabled }) => ({
    position: "relative",
    overflow: "hidden",
    height: 46,
    margin: "3px 12px",
    padding: "0 14px",
    border: "1px solid transparent",
    borderRadius: 12,
    fontSize: 15,
    color: disabled ? "var(--color-muted)" : "var(--color-shell-text)",
    opacity: disabled ? 0.6 : 1,
    transition: "background-color 0.15s ease, color 0.15s ease",
    "&:hover": { backgroundColor: "rgb(255 255 255 / 0.04)", color: "#ffffff" },
    "&.active": {
      backgroundColor: "rgb(255 255 255 / 0.010)",
      borderColor: "rgb(255 255 255 / 0.08)",
      color: "#ffffff",
      fontWeight: 600,
    },
    "&.active::before": {
      content: '""',
      position: "absolute",
      left: "50%",
      top: -18,
      width: "60%",
      height: 40,
      transform: "translateX(-50%)",
      background: "radial-gradient(ellipse at center, rgb(33 111 176 / 0.9), rgb(33 111 176 / 0) 75%)",
      filter: "blur(5px)",
      pointerEvents: "none",
    },
  }),
  icon: { minWidth: 26, width: 26, marginRight: 10 },
};

export function AppSidebar() {
  const { user, signOut } = useAuth();
  const visibleItems = navigationItems.filter((item) => item.roles.includes(user.role));

  return (
    <Sidebar width="240px" backgroundColor="transparent" rootStyles={sidebarRootStyles}>
      <div className={styles.brand}>
        <img className={styles.logo} src="/sublime-logo-symbol.png" alt="Sublime Fisioterapia" />
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
