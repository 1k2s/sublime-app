// Tela de login
import { useNavigate } from "react-router-dom";
import { LoginForm } from "../components/LoginForm";
import styles from "./LoginPage.module.css";

export function LoginPage() {
  const navigate = useNavigate();

  // Provisório: ainda não autentica, só entra no app. O login de verdade entra com o AuthContext
  function handleSubmit() {
    navigate("/");
  }

  return (
    <main className={styles.page}>
      <section className={styles.card}>
        <div className={styles.brand}>
          <span className={styles.brandName}>Sublime</span>
          <span className={styles.brandTagline}>Fisioterapia Especializada</span>
        </div>

        <h1 className={styles.title}>Acesse sua conta</h1>

        <LoginForm onSubmit={handleSubmit} />
      </section>
    </main>
  );
}
