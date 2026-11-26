// Nível da barra de força da nova senha, sobre os critérios da senha forte (passwordRule, no auth).
// Só "Forte" (os quatro critérios) é aceito pela API; os outros níveis mostram o quanto falta
import { PASSWORD_CRITERIA } from "@/domain/auth/schemas/passwordRule";

// filled: quantos dos três segmentos da barra ficam preenchidos
const STRENGTH_LEVELS = {
  weak: { key: "weak", label: "Fraca", filled: 1 },
  medium: { key: "medium", label: "Média", filled: 2 },
  strong: { key: "strong", label: "Forte", filled: 3 },
};

// Abaixo do mínimo é sempre fraca; acima, conta os outros critérios (maiúsculas e minúsculas,
// número, especial): até um, fraca; dois, média; os três, forte. Vazia: null (barra apagada)
export function getPasswordStrength(password) {
  if (!password) return null;

  const [lengthCriterion, ...varietyCriteria] = PASSWORD_CRITERIA;
  if (!lengthCriterion.test(password)) return STRENGTH_LEVELS.weak;

  const metVariety = varietyCriteria.filter((criterion) => criterion.test(password)).length;
  if (metVariety === varietyCriteria.length) return STRENGTH_LEVELS.strong;
  return metVariety === varietyCriteria.length - 1 ? STRENGTH_LEVELS.medium : STRENGTH_LEVELS.weak;
}
