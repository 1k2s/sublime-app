import { useState } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { showError } from "@/shared/utils/alert";
import { useLogin } from "../api/useLogin";
import { useFirstAccess } from "../api/useFirstAccess";
import { LoginForm } from "../components/LoginForm";
import { FirstAccessForm } from "../components/FirstAccessForm";
import { useAuth } from "../context/useAuth";
import styles from "./LoginPage.module.css";

export function LoginPage() {
  const { user, signIn } = useAuth();
  const { mutate: login, isPending: isLoggingIn } = useLogin();
  const { mutate: completeFirstAccess, isPending: isCompletingFirstAccess } = useFirstAccess();
  const [firstAccessCredentials, setFirstAccessCredentials] = useState(null);
  const location = useLocation();

  if (user) {
    return <Navigate to={location.state?.from?.pathname ?? "/"} replace />;
  }

  function handleLoginSubmit(credentials) {
    login(credentials, {
      onSuccess: signIn,
      onError: (error) => {
        if (error?.response?.data?.firstAccessRequired === true) {
          setFirstAccessCredentials({
            email: credentials.email,
            provisionalPassword: credentials.password,
          });
          return;
        }
        showError(error.message);
      },
    });
  }

  function handleFirstAccessSubmit({ newPassword }) {
    if (!firstAccessCredentials) return;

    completeFirstAccess(
      {
        email: firstAccessCredentials.email,
        provisionalPassword: firstAccessCredentials.provisionalPassword,
        newPassword,
      },
      {
        onSuccess: signIn,
        onError: (error) => showError(error.message),
      }
    );
  }

  return (
    <main className={styles.page}>
      <section className={styles.card}>
        <div className={styles.brand}>
          <span className={styles.brandName}>Sublime</span>
          <span className={styles.brandTagline}>Fisioterapia Especializada</span>
        </div>

        <h1 className={styles.title}>
          {firstAccessCredentials ? "Defina sua senha pessoal" : "Acesse sua conta"}
        </h1>

        {firstAccessCredentials ? (
          <FirstAccessForm
            email={firstAccessCredentials.email}
            onSubmit={handleFirstAccessSubmit}
            onBack={() => setFirstAccessCredentials(null)}
            isSubmitting={isCompletingFirstAccess}
          />
        ) : (
          <LoginForm onSubmit={handleLoginSubmit} isSubmitting={isLoggingIn} />
        )}
      </section>
    </main>
  );
}
