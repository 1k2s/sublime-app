// Validação do formulário de Configurações (só formato). A senha atual fica fora: é pedida no
// modal do salvamento (CurrentPasswordModal) e conferida pela API
import { z } from "zod";
import { strongPassword } from "@/domain/auth/schemas/passwordRule";

export const settingsSchema = z
  .object({
    email: z.string().trim().min(1, "Informe o e-mail").pipe(z.email("E-mail inválido")),
    // Vazia = mantém a senha atual
    newPassword: z.union([z.literal(""), strongPassword("A nova senha")]),
    confirmNewPassword: z.string(),
  })
  .refine((values) => values.newPassword === values.confirmNewPassword, {
    message: "A confirmação não confere com a nova senha",
    path: ["confirmNewPassword"],
  });
