// Formulário de Configurações: e-mail e troca de senha do próprio usuário, confirmando a
// senha atual. Só valida e entrega os dados: quem salva é a page. Junto com os valores, entrega
// resetPasswords, para a page limpar as senhas só depois que a API confirmar
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Button } from "@/shared/ui/Button";
import { FormField } from "@/shared/ui/FormField";
import { Input } from "@/shared/ui/Input";
import { settingsSchema } from "../schemas/settingsSchema";
import styles from "./SettingsForm.module.css";

const EMPTY_PASSWORDS = { newPassword: "", confirmNewPassword: "", currentPassword: "" };

export function SettingsForm({ defaultEmail, onSubmit, isSubmitting = false }) {
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(settingsSchema),
    defaultValues: { email: defaultEmail, ...EMPTY_PASSWORDS },
  });

  function submit(values) {
    const resetPasswords = () => reset({ email: values.email, ...EMPTY_PASSWORDS });
    onSubmit(values, { resetPasswords });
  }

  return (
    <form className={styles.form} onSubmit={handleSubmit(submit)} noValidate>
      <FormField label="E-mail" htmlFor="settings-email" error={errors.email?.message}>
        <Input
          id="settings-email"
          type="email"
          autoComplete="username"
          invalid={Boolean(errors.email)}
          {...register("email")}
        />
      </FormField>

      <FormField label="Nova senha (deixe em branco para manter a atual)" htmlFor="settings-new-password" error={errors.newPassword?.message}>
        <Input
          id="settings-new-password"
          type="password"
          autoComplete="new-password"
          invalid={Boolean(errors.newPassword)}
          {...register("newPassword")}
        />
      </FormField>

      <FormField label="Confirmação da nova senha" htmlFor="settings-confirm-new-password" error={errors.confirmNewPassword?.message}>
        <Input
          id="settings-confirm-new-password"
          type="password"
          autoComplete="new-password"
          invalid={Boolean(errors.confirmNewPassword)}
          {...register("confirmNewPassword")}
        />
      </FormField>

      <FormField label="Senha atual" htmlFor="settings-current-password" error={errors.currentPassword?.message}>
        <Input
          id="settings-current-password"
          type="password"
          autoComplete="current-password"
          invalid={Boolean(errors.currentPassword)}
          {...register("currentPassword")}
        />
      </FormField>

      <Button type="submit" className={styles["form-submit"]} loading={isSubmitting}>
        Salvar
      </Button>
    </form>
  );
}
