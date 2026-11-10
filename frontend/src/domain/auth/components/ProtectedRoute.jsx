import { Navigate, Outlet, useLocation } from "react-router-dom";
import { getHomePath } from "@/domain/auth/roles";
import { useAuth } from "../context/useAuth";

// Envolve rotas que exigem login. Com roles, só esses papéis entram: os outros vão para a
// tela inicial do próprio papel. É só experiência de uso: a proteção real é o 403 da API
// (o papel guardado no navegador pode ser editado)
export function ProtectedRoute({ roles }) {
  const { user } = useAuth();
  const location = useLocation();

  if (!user) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  if (roles && !roles.includes(user.role)) {
    return <Navigate to={getHomePath(user.role)} replace />;
  }

  return <Outlet />;
}
