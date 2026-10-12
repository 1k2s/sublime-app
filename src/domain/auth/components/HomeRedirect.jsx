import { Navigate } from "react-router-dom";
import { useAuth } from "../context/useAuth";
import { getHomePath } from "../roles";

export function HomeRedirect() {
  const { user } = useAuth();
  return <Navigate to={getHomePath(user.role)} replace />;
}
