// Formulário de Configurações: e-mail e troca de senha do próprio usuário, confirmando a
// senha atual. Uma linha por ajuste (explicação à esquerda, campos à direita) e um rodapé com o
// estado das alterações. Só valida e entrega os dados: quem salva é a page. Junto com os valores,
// entrega resetPasswords, para a page limpar as senhas só depois que a API confirmar
import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { KeyRound, Mail } from "lucide-react";
import { Button } from "@/shared/components/Button";
import { FormField } from "@/shared/components/FormField";
import { Input } from "@/shared/components/Input";
import { PasswordInput } from "@/shared/components/PasswordInput";
import { settingsSchema } from "../schemas/settingsSchema";
import { PasswordRequirements } from "./PasswordRequirements";
import styles from "./SettingsForm.module.css";

const EMPTY_PASSWORDS = { newPassword: "", confirmNewPassword: "", currentPassword: "" };

export function SettingsForm({ defaultEmail, onSubmit, isSubmitting = false }) {
  const {
    register,
    handleSubmit,
    reset,
    control,
    formState: { errors, dirtyFields },
  } = useForm({
    resolver: zodResolver(settingsSchema),
    defaultValues: { email: defaultEmail, ...EMPTY_PASSWORDS },
  });
  const [newPassword, confirmNewPassword] = useWatch({ control, name: ["newPassword", "confirmNewPassword"] });

  // A senha atual só confirma: digitá-la sozinha não é uma alteração a salvar
  const hasChanges = Boolean(dirtyFields.email || dirtyFields.newPassword || dirtyFields.confirmNewPassword);

  function submit(values) {
    const resetPasswords = () => reset({ email: values.email, ...EMPTY_PASSWORDS });
    onSubmit(values, { resetPasswords });
  }

  return (
    <form onSubmit={handleSubmit(submit)} noValidate>
      <SettingsRow title="E-mail de acesso" description="Usado para entrar no sistema. Precisa ser único entre os usuários.">
        <FormField label="E-mail" htmlFor="settings-email" error={errors.email?.message}>
          <Input
            id="settings-email"
            type="email"
            autoComplete="username"
            icon={Mail}
            invalid={Boolean(errors.email)}
            {...register("email")}
          />
        </FormField>
      </SettingsRow>

      <SettingsRow title="Nova senha" description="Opcional. Em branco, a senha atual continua valendo.">
        <FormField label="Nova senha" htmlFor="settings-new-password" error={errors.newPassword?.message}>
          <PasswordInput
            id="settings-new-password"
            autoComplete="new-password"
            invalid={Boolean(errors.newPassword)}
            {...register("newPassword")}
          />
        </FormField>

        <FormField label="Confirmação da nova senha" htmlFor="settings-confirm-new-password" error={errors.confirmNewPassword?.message}>
          <PasswordInput
            id="settings-confirm-new-password"
            autoComplete="new-password"
            invalid={Boolean(errors.confirmNewPassword)}
            {...register("confirmNewPassword")}
          />
        </FormField>

        <PasswordRequirements newPassword={newPassword} confirmNewPassword={confirmNewPassword} />
      </SettingsRow>

      <SettingsRow
        title="Confirme que é você"
        description="A senha atual é pedida para salvar qualquer alteração desta tela."
        highlighted
      >
        <FormField label="Senha atual" htmlFor="settings-current-password" error={errors.currentPassword?.message}>
          <PasswordInput
            id="settings-current-password"
            autoComplete="current-password"
            icon={KeyRound}
            invalid={Boolean(errors.currentPassword)}
            {...register("currentPassword")}
          />
        </FormField>
      </SettingsRow>

      <footer className={styles.footer}>
        <p className={[styles.status, hasChanges && styles["status-dirty"]].filter(Boolean).join(" ")} aria-live="polite">
          <span className={styles["status-dot"]} aria-hidden="true" />
          {hasChanges ? "Alterações não salvas" : "Nenhuma alteração"}
        </p>

        <div className={styles.actions}>
          <Button variant="outline" onClick={() => reset()} disabled={!hasChanges || isSubmitting}>
            Descartar
          </Button>
          <Button type="submit" loading={isSubmitting} disabled={!hasChanges}>
            Salvar alterações
          </Button>
        </div>
      </footer>
    </form>
  );
}

// Uma linha do formulário: o que é o ajuste, à esquerda; os campos, à direita
function SettingsRow({ title, description, highlighted = false, children }) {
  return (
    <section className={[styles.row, highlighted && styles["row-highlighted"]].filter(Boolean).join(" ")}>
      <div className={styles["row-text"]}>
        <h2 className={styles["row-title"]}>{title}</h2>
        <p className={styles["row-description"]}>{description}</p>
      </div>
      <div className={styles["row-fields"]}>{children}</div>
    </section>
  );
}
