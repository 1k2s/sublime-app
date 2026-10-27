// Itens do menu lateral. O campo roles indica quais papéis enxergam cada item
import { CalendarCheck, FileText, House, Settings, Tag, User } from "lucide-react";
import { ROLES } from "@/domain/auth/roles";

const ADMIN = ROLES.ADMIN.value;
const PROVIDER = ROLES.PROVIDER.value;

// Itens sem tela pronta ficam com disabled: true
export const navigationItems = [
  { label: "Dashboard", path: "/dashboard", icon: House, roles: [ADMIN], disabled: true },
  { label: "Lançamentos", path: "/consultations", icon: CalendarCheck, roles: [PROVIDER] },
  { label: "Pacientes", path: "/patients", icon: User, roles: [ADMIN, PROVIDER] },
  { label: "Contratos", path: "/contracts", icon: FileText, roles: [ADMIN] },
  { label: "Preços", path: "/pricing", icon: Tag, roles: [ADMIN] },
  { label: "Configurações", path: "/settings", icon: Settings, roles: [ADMIN, PROVIDER], disabled: true },
];
