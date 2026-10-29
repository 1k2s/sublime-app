// Formulário de login. Só valida e entrega os dados: quem autentica é a page
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Button } from "@/shared/ui/Button";
import { FormField } from "@/shared/ui/FormField";
import { Input } from "@/shared/ui/Input";
import { loginSchema } from "../schemas/loginSchema";
import styles from "./LoginForm.module.css";

export function LoginForm({ onSubmit, isSubmitting = false }) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: "", password: "" },
  });

  return (
    <form className={styles.form} onSubmit={handleSubmit(onSubmit)} noValidate>
      <FormField label="E-mail" htmlFor="login-email" error={errors.email?.message}>
        <Input
          id="login-email"
          type="email"
          autoComplete="username"
          placeholder="voce@exemplo.com"
          invalid={Boolean(errors.email)}
          {...register("email")}
        />
      </FormField>

      <FormField label="Senha" htmlFor="login-password" error={errors.password?.message}>
        <Input
          id="login-password"
          type="password"
          autoComplete="current-password"
          invalid={Boolean(errors.password)}
          {...register("password")}
        />
      </FormField>

      <Button type="submit" className={styles["form-submit"]} loading={isSubmitting}>
        Entrar
      </Button>
    </form>
  );
}
