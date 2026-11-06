// Validação de formato do formulário de paciente, espelhando o PatientRequestDTO.
// A validação que vale é a da API; aqui é só para dar resposta rápida na tela.
import { z } from "zod";
import { isValid, parse } from "date-fns";
import { onlyDigits, isValidCpf } from "@/shared/utils/masks";
import { hasAddress } from "../patientForm";

function isRealDate(maskedDate) {
  return isValid(parse(maskedDate, "dd/MM/yyyy", new Date()));
}

// Etapa 1: dados pessoais. Telefone e e-mail são opcionais na API
export const personalDataSchema = z.object({
  name: z
    .string()
    .trim()
    .min(3, "O nome deve ter pelo menos 3 caracteres")
    .max(255, "O nome deve ter no máximo 255 caracteres"),

  cpf: z.string().min(1, "CPF é obrigatório").refine(isValidCpf, "CPF inválido"),

  birthDate: z
    .string()
    .min(1, "Data de nascimento é obrigatória")
    .refine(isRealDate, "Data inválida (DD/MM/AAAA)"),

  phone: z
    .string()
    .refine((val) => onlyDigits(val).length === 0 || onlyDigits(val).length >= 10, "Telefone inválido"),

  email: z.union([z.literal(""), z.string().trim().email("E-mail inválido")]),
});

// Etapa 2: endereço. Opcional, mas, se algum campo for preenchido, precisa estar completo
// (só o complemento é livre), como no AddressDTO
export const addressSchema = z.object({
  cep: z.string(),
  street: z.string(),
  numberHouse: z.string(),
  complement: z.string(),
  city: z.string(),
});

function requireCompleteAddress(values, ctx) {
  if (!hasAddress(values)) return;

  if (onlyDigits(values.cep).length !== 8) {
    ctx.addIssue({ code: "custom", path: ["cep"], message: "CEP deve conter 8 dígitos" });
  }
  if (!values.street.trim()) {
    ctx.addIssue({ code: "custom", path: ["street"], message: "Logradouro é obrigatório" });
  }
  if (!values.numberHouse.trim()) {
    ctx.addIssue({ code: "custom", path: ["numberHouse"], message: "Número é obrigatório" });
  }
  if (!values.city.trim()) {
    ctx.addIssue({ code: "custom", path: ["city"], message: "Cidade é obrigatória" });
  }
}

// Formulário inteiro (as duas etapas)
export const fullPatientSchema = personalDataSchema
  .extend(addressSchema.shape)
  .superRefine(requireCompleteAddress);
