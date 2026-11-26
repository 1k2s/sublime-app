// Validação do formulário de Configurações (só formato: a senha atual é conferida pela API)
import { z } from "zod";

// O mesmo mínimo da API; a tela também o mostra na conferência da nova senha (PasswordRequirements)
export const MIN_PASSWORD_LENGTH = 8;

export const settingsSchema = z
  .object({
    email: z.string().trim().min(1, "Informe o e-mail").pipe(z.email("E-mail inválido")),
    // Vazia = mantém a senha atual
    newPassword: z
      .string()
      .refine(
        (value) => value === "" || value.length >= MIN_PASSWORD_LENGTH,
        `A nova senha deve ter no mínimo ${MIN_PASSWORD_LENGTH} caracteres`
      ),
    confirmNewPassword: z.string(),
    currentPassword: z.string().min(1, "Informe a senha atual"),
  })
  .refine((values) => values.newPassword === values.confirmNewPassword, {
    message: "A confirmação não confere com a nova senha",
    path: ["confirmNewPassword"],
  });
