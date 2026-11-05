/**
 * ======================================================================================
 * SCHEMAS DE VALIDAÇÃO ZOD: DOMÍNIO PATIENT
 * ======================================================================================
 *
 * Como o projeto utiliza JavaScript puro (sem TypeScript), o Zod é a principal camada
 * que garante a integridade e validação dos dados antes de trafegarem para a API.
 *
 * Os schemas são divididos por etapa do formulário wizard para permitir validações parciais
 * ao avançar de etapa, além de fornecer o schema completo consolidado (`fullPatientSchema`).
 */

import { z } from "zod";
import { onlyDigits, isValidCpf } from "@/shared/utils/masks";

/**
 * Validação da Etapa 1: Dados Pessoais de identificação do paciente.
 */
export const personalDataSchema = z.object({
  // Nome completo com no mínimo 3 caracteres
  name: z.string().trim().min(3, "Nome completo deve ter pelo menos 3 caracteres"),

  // CPF obrigatório com validação oficial dos dígitos verificadores (evita CPFs falsos)
  cpf: z
    .string()
    .min(1, "CPF é obrigatório")
    .refine(isValidCpf, "CPF inválido ou não existe"),

  // Data de nascimento obrigatória com exatamente 8 dígitos numéricos (DD/MM/AAAA)
  birthDate: z
    .string()
    .min(1, "Data de nascimento é obrigatória")
    .refine(
      (val) => onlyDigits(val).length === 8,
      "Data de nascimento deve conter 8 dígitos (DD/MM/AAAA)"
    ),

  // Telefone obrigatório (fixo ou celular, com no mínimo 10 dígitos com DDD)
  phone: z
    .string()
    .min(1, "Telefone é obrigatório")
    .refine((val) => onlyDigits(val).length >= 10, "Telefone inválido"),

  // E-mail obrigatório com formato de e-mail válido
  email: z
    .string()
    .trim()
    .min(1, "E-mail é obrigatório")
    .email("E-mail inválido"),
});

/**
 * Validação da Etapa 2: Endereço de residência do paciente.
 */
export const addressSchema = z.object({
  // CEP obrigatório com exatamente 8 dígitos numéricos
  cep: z
    .string()
    .min(1, "CEP é obrigatório")
    .refine((val) => onlyDigits(val).length === 8, "CEP deve conter 8 dígitos"),

  // Logradouro (rua, avenida, etc.)
  street: z.string().trim().min(1, "Logradouro é obrigatório"),

  // Número do imóvel (ou S/N)
  number: z.string().trim().min(1, "Número é obrigatório"),

  // Complemento (apartamento, bloco, etc. - campo opcional)
  complement: z.string().optional().or(z.literal("")),

  // Bairro
  neighborhood: z.string().trim().min(1, "Bairro é obrigatório"),

  // Cidade / Município
  city: z.string().trim().min(1, "Cidade é obrigatória"),

  // Sigla da Unidade Federativa (UF), exatamente 2 letras
  state: z.string().trim().min(2, "Estado é obrigatório").max(2, "UF deve ter 2 letras"),
});

/**
 * Schema consolidado de todas as etapas.
 * Utilizado para validação integral na submissão final do formulário.
 */
export const fullPatientSchema = personalDataSchema.merge(addressSchema).extend({
  // Status inicial do cadastro: ativo por padrão
  status: z.enum(["ACTIVE", "INACTIVE"]).default("ACTIVE"),
});
