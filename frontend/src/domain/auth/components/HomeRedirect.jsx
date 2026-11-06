import { Navigate } from "react-router-dom";
import { getHomePath } from "@/domain/auth/roles";
import { useAuth } from "@/domain/auth/context/useAuth";

// Rota "/": leva o usuário à tela inicial do perfil dele (ADMIN → /patients, PROVIDER → /consultations).
// Fica dentro do ProtectedRoute, então sempre há usuário logado aqui
export function HomeRedirect() {
  const { user } = useAuth();
  return <Navigate to={getHomePath(user.role)} replace />;
}
