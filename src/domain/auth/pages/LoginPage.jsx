import { Navigate, useLocation } from "react-router-dom";
import { showError } from "@/shared/utils/alert";
import { useLogin } from "../api/useLogin";
import { LoginForm } from "../components/LoginForm";
import { useAuth } from "../context/useAuth";
import styles from "./LoginPage.module.css";

export function LoginPage() {
  const { user, signIn } = useAuth();
  const { mutate: login, isPending } = useLogin();
  const location = useLocation();

  if (user) {
    return <Navigate to={location.state?.from?.pathname ?? "/"} replace />;
  }

  function handleSubmit(credentials) {
    login(credentials, {
      onSuccess: signIn,
      onError: (error) => showError(error.message),
    });
  }

  return (
    <main className={styles.page}>
      <section className={styles.card}>
        <div className={styles.brand}>
          <span className={styles.brandName}>Sublime</span>
          <span className={styles.brandTagline}>Fisioterapia Especializada</span>
        </div>

        <h1 className={styles.title}>Acesse sua conta</h1>

        <LoginForm onSubmit={handleSubmit} isSubmitting={isPending} />
      </section>
    </main>
  );
}
