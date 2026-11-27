import { useState } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { showError } from "@/shared/utils/alert";
import { useLogin } from "../api/useLogin";
import { useFirstAccess } from "../api/useFirstAccess";
import { LoginForm } from "../components/LoginForm";
import { FirstAccessForm } from "../components/FirstAccessForm";
import { useAuth } from "../context/useAuth";
import { CalendarCheck, FileText, ShieldCheck, Tag } from "lucide-react";
import styles from "./LoginPage.module.css";

// Destaques do painel da marca: só o que o sistema faz hoje
const HERO_FEATURES = [
  { icon: CalendarCheck, label: "Atendimentos e repasses sempre em dia" },
  { icon: FileText, label: "Contratos e saldo de sessões de cada paciente" },
  { icon: Tag, label: "Tabela de preços por plano e técnica" },
];

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
      <aside className={styles.hero} aria-hidden="true">
        <img className={styles.heroLogoSmall} src="/sublime-logo-symbol.png" alt="" />
        <img className={styles.heroLogoBackground} src="/sublime-logo-symbol.png" alt="" />

        <div className={styles.heroContent}>
          <p className={styles.heroTitle}>Tudo da clínica em um só lugar.</p>
          <p className={styles.heroText}>
            Pacientes, contratos e valores organizados para a equipe cuidar do que importa: as pessoas.
          </p>
          <ul className={styles.features}>
            {HERO_FEATURES.map(({ icon: Icon, label }) => (
              <li key={label} className={styles.feature}>
                <span className={styles.featureIcon}>
                  <Icon size={18} />
                </span>
                {label}
              </li>
            ))}
          </ul>
        </div>

        <p className={styles.heroFooter}>© {new Date().getFullYear()} Sublime Fisioterapia Especializada</p>
      </aside>

      <div className={styles.formSide}>
      <section className={styles.card}>
        <div className={styles.brand}>
          <span className={styles.brandName}>Sublime</span>
          <span className={styles.brandTagline}>Fisioterapia Especializada</span>
        </div>

        <h1 className={styles.title}>
          {firstAccessCredentials ? "Defina sua senha pessoal" : "Acesse sua conta"}
        </h1>
        <p className={styles.subtitle}>
          {firstAccessCredentials
            ? "Troque a senha provisória para continuar."
            : "Entre com seu e-mail e senha para continuar."}
        </p>

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

        <p className={styles.cardFooter}>
          <ShieldCheck size={14} aria-hidden />
          Acesso restrito a profissionais autorizados
        </p>
      </section>
      </div>
    </main>
  );
}
