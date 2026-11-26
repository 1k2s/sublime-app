import { z } from "zod";
import { ROLES } from "@/domain/auth/roles";
import { strongPassword } from "@/domain/auth/schemas/passwordRule";

const ROLE_VALUES = Object.values(ROLES).map((role) => role.value);

const email = z.string().trim().min(1, "E-mail é obrigatório").email("E-mail inválido");
const role = z.enum(ROLE_VALUES, { message: "Selecione o perfil" });
const password = strongPassword("A senha");

export const createUserSchema = z.object({ email, password, role });

// Na alteração, senha vazia mantém a atual
export const updateUserSchema = z.object({
  email,
  role,
  password: z.union([z.literal(""), password]),
});
