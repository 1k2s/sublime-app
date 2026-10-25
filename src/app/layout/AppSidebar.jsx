// Menu lateral: marca, itens de navegação e usuário logado
import { NavLink } from "react-router-dom";
import { Menu, MenuItem, Sidebar, sidebarClasses } from "react-pro-sidebar";
import { LogOut, User } from "lucide-react";
import { useAuth } from "@/domain/auth/context/useAuth";
import { getRole } from "@/domain/auth/roles";
import { navigationItems } from "./navigationItems";
import styles from "./AppSidebar.module.css";

const sidebarRootStyles = {
  borderRight: "1px solid #e5e0d5",
  [`.${sidebarClasses.container}`]: {
    display: "flex",
    flexDirection: "column",
  },
};

const menuItemStyles = {
  button: ({ disabled }) => ({
    height: 40,
    margin: "2px 12px",
    padding: "0 12px",
    borderLeft: "3px solid transparent",
    borderRadius: 8,
    fontSize: 14,
    color: disabled ? "#6b7280" : "#374151",
    opacity: disabled ? 0.6 : 1,
    "&:hover": { backgroundColor: "#dfe8dc" },
    "&.active": {
      backgroundColor: "#dfe8dc",
      borderLeftColor: "#3f6fb5",
      fontWeight: 500,
    },
  }),
  icon: { minWidth: 24, width: 24, marginRight: 8 },
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
            icon={<Icon size={18} />}
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
          <span className={styles.userName}>{user.name}</span>
          <span className={styles.userRole}>{getRole(user.role)?.label}</span>
        </div>
        <button type="button" className={styles.logout} aria-label="Sair" title="Sair" onClick={signOut}>
          <LogOut size={18} />
        </button>
      </div>
    </Sidebar>
  );
}
