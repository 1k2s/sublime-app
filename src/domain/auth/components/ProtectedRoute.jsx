import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "../context/useAuth";
import { getHomePath } from "../roles";

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
