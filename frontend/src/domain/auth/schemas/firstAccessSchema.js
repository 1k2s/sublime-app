// Validação do formulário de definição de senha pessoal no primeiro acesso
import { z } from "zod";
import { strongPassword } from "./passwordRule";

export const firstAccessSchema = z
  .object({
    newPassword: strongPassword("A nova senha"),
    confirmPassword: z
      .string()
      .min(1, "Confirme a nova senha"),
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    message: "As senhas não coincidem",
    path: ["confirmPassword"],
  });
