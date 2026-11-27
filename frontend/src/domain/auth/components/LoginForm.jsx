// Formulário de login. Só valida e entrega os dados: quem autentica é a page
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Button } from "@/shared/components/Button";
import { FormField } from "@/shared/components/FormField";
import { ArrowRight, Mail } from "lucide-react";
import { Input } from "@/shared/components/Input";
import { PasswordInput } from "@/shared/components/PasswordInput";
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
          icon={Mail}
          autoComplete="username"
          placeholder="voce@exemplo.com"
          invalid={Boolean(errors.email)}
          {...register("email")}
        />
      </FormField>

      <FormField label="Senha" htmlFor="login-password" error={errors.password?.message}>
        <PasswordInput
          id="login-password"
          autoComplete="current-password"
          placeholder="Digite sua senha"
          invalid={Boolean(errors.password)}
          {...register("password")}
        />
      </FormField>

      <Button type="submit" className={styles["form-submit"]} loading={isSubmitting}>
        Entrar
        <ArrowRight size={16} aria-hidden />
      </Button>
    </form>
  );
}
