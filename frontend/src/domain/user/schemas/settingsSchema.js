// Validação do formulário de Configurações (só formato: a senha atual é conferida pela API)
import { z } from "zod";

export const settingsSchema = z
  .object({
    email: z.string().trim().min(1, "Informe o e-mail").pipe(z.email("E-mail inválido")),
    // Vazia = mantém a senha atual
    newPassword: z.string().refine((value) => value === "" || value.length >= 8, "A nova senha deve ter no mínimo 8 caracteres"),
    confirmNewPassword: z.string(),
    currentPassword: z.string().min(1, "Informe a senha atual"),
  })
  .refine((values) => values.newPassword === values.confirmNewPassword, {
    message: "A confirmação não confere com a nova senha",
    path: ["confirmNewPassword"],
  });
