/**
 * ======================================================================================
 * UTILITÁRIOS: MÁSCARAS DE ENTRADA (MASKS)
 * ======================================================================================
 *
 * Funções puras para sanitização e formatação visual em tempo real de campos de texto:
 * - `onlyDigits`: Remove caracteres não numéricos.
 * - `maskCpf`: Formata CPF no padrão 000.000.000-00.
 * - `maskPhone`: Formata telefones celulares (11 dígitos) e fixos (10 dígitos).
 * - `maskCep`: Formata CEP no padrão 00000-000.
 * - `maskDate`: Formata data de nascimento no padrão DD/MM/AAAA.
 */

/**
 * Remove qualquer caractere que não seja dígito numérico (0-9).
 *
 * @param {string|number} value - Valor de entrada
 * @returns {string} Sequência contendo apenas números
 */
export function onlyDigits(value) {
  return String(value ?? "").replace(/\D/g, "");
}

/**
 * Aplica máscara de CPF: "12345678901" -> "123.456.789-01"
 *
 * @param {string} value - Valor digitado
 * @returns {string} CPF formatado
 */
export function maskCpf(value) {
  return onlyDigits(value)
    .slice(0, 11)
    .replace(/(\d{3})(\d)/, "$1.$2")
    .replace(/(\d{3})(\d)/, "$1.$2")
    .replace(/(\d{3})(\d{1,2})$/, "$1-$2");
}

/**
 * Aplica máscara de telefone:
 * Celular (11 dígitos): "(16) 99123-4567"
 * Fixo (10 dígitos): "(16) 3345-6789"
 *
 * @param {string} value - Valor digitado
 * @returns {string} Telefone formatado
 */
export function maskPhone(value) {
  const digits = onlyDigits(value).slice(0, 11);
  if (digits.length <= 2) return digits;
  if (digits.length <= 6) return `(${digits.slice(0, 2)}) ${digits.slice(2)}`;
  const prefixLength = digits.length === 11 ? 5 : 4;
  return `(${digits.slice(0, 2)}) ${digits.slice(2, 2 + prefixLength)}-${digits.slice(2 + prefixLength)}`;
}

/**
 * Aplica máscara de CEP: "13560049" -> "13560-049"
 *
 * @param {string} value - Valor digitado
 * @returns {string} CEP formatado
 */
export function maskCep(value) {
  return onlyDigits(value)
    .slice(0, 8)
    .replace(/(\d{5})(\d)/, "$1-$2");
}

/**
 * Aplica máscara de data com barras: "14031990" -> "14/03/1990"
 *
 * @param {string} value - Valor digitado
 * @returns {string} Data formatada DD/MM/AAAA
 */
export function maskDate(value) {
  const digits = onlyDigits(value).slice(0, 8);
  if (digits.length <= 2) return digits;
  if (digits.length <= 4) return `${digits.slice(0, 2)}/${digits.slice(2)}`;
  return `${digits.slice(0, 2)}/${digits.slice(2, 4)}/${digits.slice(4)}`;
}
