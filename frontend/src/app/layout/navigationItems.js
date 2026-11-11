// Itens do menu lateral. O campo roles indica quais papéis enxergam cada item (só visual: quem
// protege é o backend, e as rotas levam quem não tem o papel à tela inicial dele)
import { CalendarCheck, FileText, House, Settings, Stethoscope, Tag, User, UserCog } from "lucide-react";
import { ROLES } from "@/domain/auth/roles";

const ADMIN = ROLES.ADMIN.value;
const PROVIDER = ROLES.PROVIDER.value;

// Itens sem tela pronta ficam com disabled: true
export const navigationItems = [
  { label: "Dashboard", path: "/dashboard", icon: House, roles: [ADMIN, PROVIDER], disabled: true },
  { label: "Pacientes", path: "/patients", icon: User, roles: [ADMIN] },
  { label: "Contratos", path: "/contracts", icon: FileText, roles: [ADMIN] },
  { label: "Preços", path: "/pricing", icon: Tag, roles: [ADMIN] },
  { label: "Lançamentos", path: "/consultations", icon: CalendarCheck, roles: [ADMIN, PROVIDER] },
  { label: "Prestadores", path: "/providers", icon: Stethoscope, roles: [ADMIN] },
  { label: "Usuários", path: "/users", icon: UserCog, roles: [ADMIN] },
  { label: "Configurações", path: "/settings", icon: Settings, roles: [ADMIN, PROVIDER] },
];
