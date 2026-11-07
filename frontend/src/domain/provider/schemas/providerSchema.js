import { z } from "zod";

const percentageRegex = /^\d{1,3}([.,]\d{1,2})?$/;

function parsePercentage(val) {
  if (typeof val === "number") return val;
  if (!val || typeof val !== "string") return NaN;
  return Number(val.trim().replace(",", "."));
}

const percentageFieldSchema = z
  .string()
  .trim()
  .min(1, "Informe o percentual de comissão")
  .refine((val) => percentageRegex.test(val), {
    message: "Use um número válido com até 2 casas decimais (ex: 40 ou 42.5)",
  })
  .refine(
    (val) => {
      const num = parsePercentage(val);
      return !Number.isNaN(num) && num >= 0 && num <= 100;
    },
    {
      message: "O percentual deve estar entre 0% e 100%",
    }
  );

export function createProviderSchema(isEditing = false) {
  return z
    .object({
      name: z
        .string()
        .trim()
        .min(1, "O nome do prestador é obrigatório")
        .min(3, "O nome deve ter pelo menos 3 caracteres"),
      commissionPercentage: percentageFieldSchema,
      referralCommissionPercentage: percentageFieldSchema,
      email: z
        .string()
        .trim()
        .min(1, "O e-mail de acesso é obrigatório")
        .pipe(z.email("Informe um endereço de e-mail válido")),
      role: z.enum(["PROVIDER", "ADMIN"], {
        message: "Selecione um perfil de acesso válido",
      }),
      password: isEditing
        ? z
            .string()
            .optional()
            .or(z.literal(""))
            .refine((val) => !val || val.length >= 8, {
              message: "A nova senha provisória deve ter no mínimo 8 caracteres",
            })
        : z
            .string()
            .min(8, "A senha provisória deve ter no mínimo 8 caracteres"),
      status: z.enum(["ACTIVE", "INACTIVE"]).default("ACTIVE"),
    })
    .refine(
      (data) => {
        const regular = parsePercentage(data.commissionPercentage);
        const referral = parsePercentage(data.referralCommissionPercentage);
        if (Number.isNaN(regular) || Number.isNaN(referral)) return true;
        return referral >= regular;
      },
      {
        message:
          "A comissão por indicação deve ser maior ou igual à comissão padrão",
        path: ["referralCommissionPercentage"],
      }
    );
}
