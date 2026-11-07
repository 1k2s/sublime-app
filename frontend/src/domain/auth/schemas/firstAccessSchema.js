// Validação do formulário de definição de senha pessoal no primeiro acesso
import { z } from "zod";

export const firstAccessSchema = z
  .object({
    newPassword: z
      .string()
      .min(8, "A nova senha deve ter no mínimo 8 caracteres"),
    confirmPassword: z
      .string()
      .min(1, "Confirme a nova senha"),
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    message: "As senhas não coincidem",
    path: ["confirmPassword"],
  });
