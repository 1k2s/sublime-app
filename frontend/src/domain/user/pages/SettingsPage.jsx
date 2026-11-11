// Configurações: o próprio usuário (ADMIN ou PROVIDER) troca o e-mail e a senha
import { useAuth } from "@/domain/auth/context/useAuth";
import { Card } from "@/shared/components/Card";
import { PageHeader } from "@/shared/components/PageHeader";
import { showError, showSuccess } from "@/shared/utils/alert";
import { useMe, useUpdateMe } from "../api/useMe";
import { SettingsForm } from "../components/SettingsForm";
import styles from "./SettingsPage.module.css";

export function SettingsPage() {
  const { updateUser } = useAuth();
  const { data: me, isPending: isLoading, isError, error } = useMe();
  const { mutate: saveMe, isPending: isSaving } = useUpdateMe();

  // A confirmação da nova senha fica no front: a API recebe só a nova e a atual
  function handleSubmit({ email, newPassword, currentPassword }, { resetPasswords }) {
    saveMe(
      { email, newPassword, currentPassword },
      {
        onSuccess: (updated) => {
          updateUser({ email: updated.email });
          resetPasswords();
          showSuccess("Dados atualizados.");
        },
        onError: (saveError) => showError(saveError.message),
      }
    );
  }

  return (
    <div className={styles.page}>
      <PageHeader title="Configurações" description="Seu e-mail de acesso e sua senha." />

      <Card className={styles.card}>
        {isLoading && <p className={styles.message}>Carregando...</p>}
        {isError && <p className={styles.message}>{error.message}</p>}
        {me && <SettingsForm defaultEmail={me.email} onSubmit={handleSubmit} isSubmitting={isSaving} />}
      </Card>
    </div>
  );
}
