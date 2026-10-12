// Validação do formulário de login
import { z } from "zod";

export const loginSchema = z.object({
  email: z.string().trim().min(1, "Informe o e-mail").pipe(z.email("E-mail inválido")),
  password: z.string().min(1, "Informe a senha"),
});
