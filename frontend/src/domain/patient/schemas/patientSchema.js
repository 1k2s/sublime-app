// Validação de formato do formulário de paciente (espelha PatientRequestDTO e AddressDTO da API)
import { z } from "zod";
import { displayDateToIso, parseDisplayDate } from "@/shared/utils/date";
import { onlyDigits } from "@/shared/utils/masks";

function isPastCalendarDate(value) {
  const parsed = parseDisplayDate(value);
  if (!parsed) return false;
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return parsed < today;
}

export function hasAddressInput(values = {}) {
  if (values.addressRequired) return true;
  return Boolean(
    onlyDigits(values.cep) ||
      values.street?.trim() ||
      values.numberHouse?.trim() ||
      values.complement?.trim() ||
      values.city?.trim()
  );
}

// Etapa 1: Dados pessoais, contato e origem do paciente
const personalDataSchema = z.object({
  name: z
    .string()
    .trim()
    .min(1, "O nome do paciente é obrigatório.")
    .min(3, "O nome do paciente deve ter entre 3 e 255 caracteres.")
    .max(255, "O nome do paciente deve ter entre 3 e 255 caracteres."),

  cpf: z
    .string()
    .min(1, "O CPF é obrigatório.")
    .refine(
      (val) => onlyDigits(val).length === 11,
      "O CPF deve conter exatamente 11 dígitos."
    ),

  birthDate: z
    .string()
    .min(1, "A data de nascimento é obrigatória.")
    .refine(
      (val) => onlyDigits(val).length === 8 && parseDisplayDate(val) !== null,
      "Informe uma data válida no formato DD/MM/AAAA."
    )
    .refine(isPastCalendarDate, "A data de nascimento deve estar no passado."),

  phone: z
    .string()
    .optional()
    .or(z.literal(""))
    .refine(
      (val) => !val || !val.trim() || onlyDigits(val).length >= 10,
      "Informe um telefone válido com DDD."
    ),

  email: z
    .string()
    .trim()
    .optional()
    .or(z.literal(""))
    .refine(
      (val) => !val || z.string().email().safeParse(val).success,
      "O e-mail informado é inválido."
    ),

  referringProviderId: z.string().optional().or(z.literal("")),
});

// Etapa 2: Endereço opcional no cadastro, mas completo quando informado (street, numberHouse, city, cep)
const addressFieldsSchema = z.object({
  cep: z.string().optional().or(z.literal("")),
  street: z.string().optional().or(z.literal("")),
  numberHouse: z.string().optional().or(z.literal("")),
  complement: z.string().optional().or(z.literal("")),
  city: z.string().optional().or(z.literal("")),
  addressRequired: z.boolean().optional(),
});

export const fullPatientSchema = personalDataSchema
  .merge(addressFieldsSchema)
  .superRefine((values, ctx) => {
    if (!hasAddressInput(values)) return;

    if (onlyDigits(values.cep).length !== 8) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ["cep"],
        message: "O CEP deve conter exatamente 8 dígitos.",
      });
    }
    if (!values.street?.trim()) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ["street"],
        message: "A rua é obrigatória.",
      });
    }
    if (!values.numberHouse?.trim()) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ["numberHouse"],
        message: "O número é obrigatório.",
      });
    }
    if (!values.city?.trim()) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ["city"],
        message: "A cidade é obrigatória.",
      });
    }
  });

// Converte os valores do formulário para o formato exato esperado pelo PatientRequestDTO
export function toPatientPayload(values) {
  const includeAddress = hasAddressInput(values);

  return {
    name: values.name.trim(),
    cpf: onlyDigits(values.cpf),
    birthDate: displayDateToIso(values.birthDate),
    phone: values.phone && onlyDigits(values.phone) ? onlyDigits(values.phone) : null,
    email: values.email?.trim() ? values.email.trim() : null,
    address: includeAddress
      ? {
          street: values.street.trim(),
          numberHouse: values.numberHouse.trim(),
          city: values.city.trim(),
          complement: values.complement?.trim() ? values.complement.trim() : null,
          cep: onlyDigits(values.cep),
        }
      : null,
    referringProviderId: values.referringProviderId ? Number(values.referringProviderId) : null,
  };
}
