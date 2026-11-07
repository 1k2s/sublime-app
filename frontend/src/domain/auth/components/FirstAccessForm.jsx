// Formulário exibido automaticamente quando o usuário loga pela primeira vez com senha provisória
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { ShieldCheck } from "lucide-react";
import { Button } from "@/shared/ui/Button";
import { FormField } from "@/shared/ui/FormField";
import { Input } from "@/shared/ui/Input";
import { firstAccessSchema } from "../schemas/firstAccessSchema";
import styles from "./FirstAccessForm.module.css";

export function FirstAccessForm({
  email,
  onSubmit,
  onBack,
  isSubmitting = false,
}) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(firstAccessSchema),
    defaultValues: { newPassword: "", confirmPassword: "" },
  });

  return (
    <form className={styles.form} onSubmit={handleSubmit(onSubmit)} noValidate>
      <div className={styles.notice}>
        <ShieldCheck size={18} className={styles.noticeIcon} />
        <p className={styles.noticeText}>
          Este é o seu primeiro acesso com senha provisória para{" "}
          <strong>{email}</strong>. Por segurança e conformidade com a LGPD,
          defina abaixo sua senha pessoal (diferente da provisória).
        </p>
      </div>

      <FormField
        label="Nova senha"
        htmlFor="first-access-new-password"
        error={errors.newPassword?.message}
      >
        <Input
          id="first-access-new-password"
          type="password"
          autoComplete="new-password"
          placeholder="Mínimo de 8 caracteres"
          invalid={Boolean(errors.newPassword)}
          {...register("newPassword")}
        />
      </FormField>

      <FormField
        label="Confirmar nova senha"
        htmlFor="first-access-confirm-password"
        error={errors.confirmPassword?.message}
      >
        <Input
          id="first-access-confirm-password"
          type="password"
          autoComplete="new-password"
          placeholder="Repita a nova senha"
          invalid={Boolean(errors.confirmPassword)}
          {...register("confirmPassword")}
        />
      </FormField>

      <div className={styles.actions}>
        <Button type="submit" loading={isSubmitting}>
          Definir senha e entrar
        </Button>
        <Button
          type="button"
          variant="outline"
          onClick={onBack}
          disabled={isSubmitting}
        >
          Voltar ao login
        </Button>
      </div>
    </form>
  );
}
