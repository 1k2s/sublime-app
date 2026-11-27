// Formato da senha forte, o mesmo do @StrongPassword do backend: no mínimo 8 caracteres, com letra
// maiúscula, letra minúscula, número e caractere especial. Usado por todos os formulários com senha
// nova (cadastro de usuário, primeiro acesso, Configurações); quem decide é a API
import { z } from "zod";

export const MIN_PASSWORD_LENGTH = 8;

// Especial é o que não é letra, número nem espaço; letras acentuadas contam como maiúscula/minúscula
export const PASSWORD_CRITERIA = [
  { key: "length", label: `Pelo menos ${MIN_PASSWORD_LENGTH} caracteres`, test: (value) => [...value].length >= MIN_PASSWORD_LENGTH },
  { key: "case", label: "Letras maiúsculas e minúsculas", test: (value) => /\p{Lu}/u.test(value) && /\p{Ll}/u.test(value) },
  { key: "number", label: "Um número", test: (value) => /\p{N}/u.test(value) },
  { key: "symbol", label: "Um caractere especial (ex: ! @ # $)", test: (value) => /[^\p{L}\p{N}\s]/u.test(value) },
];

export function isStrongPassword(value) {
  return PASSWORD_CRITERIA.every((criterion) => criterion.test(value));
}

// subject: o começo da mensagem ("A senha", "A nova senha")
export function strongPasswordMessage(subject = "A senha") {
  return `${subject} deve ter no mínimo ${MIN_PASSWORD_LENGTH} caracteres, com letra maiúscula, letra minúscula, número e caractere especial`;
}

// Campo de senha nova obrigatória
export function strongPassword(subject) {
  return z.string().refine(isStrongPassword, strongPasswordMessage(subject));
}
