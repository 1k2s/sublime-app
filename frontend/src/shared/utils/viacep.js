/**
 * ======================================================================================
 * UTILITÁRIOS: CONSULTA DE CEP VIA VIACEP
 * ======================================================================================
 *
 * Integração com o serviço público e gratuito do ViaCEP (https://viacep.com.br):
 * - Sanitiza o CEP recebido para garantir apenas 8 dígitos numéricos.
 * - Retorna o endereço estruturado (logradouro, bairro, cidade, UF e complemento).
 * - Em caso de erro na rede ou CEP não localizado, trata a exceção com resiliência
 *   e retorna `null`, sem interromper a execução do formulário.
 */

import { onlyDigits } from "./masks";

/**
 * Consulta os dados de endereço correspondentes a um CEP.
 *
 * @param {string} cep - CEP informado pelo usuário (com ou sem máscara)
 * @returns {Promise<{street: string, neighborhood: string, city: string, state: string, complement: string}|null>}
 */
export async function fetchAddressByCep(cep) {
  const cleanCep = onlyDigits(cep);

  // Validação preliminar: o CEP deve ter exatamente 8 dígitos numéricos
  if (cleanCep.length !== 8) {
    return null;
  }

  try {
    const response = await fetch(`https://viacep.com.br/ws/${cleanCep}/json/`);
    if (!response.ok) {
      return null;
    }

    const data = await response.json();

    // A API do ViaCEP retorna { erro: "true" } quando o CEP não é encontrado na base dos Correios
    if (data.erro) {
      return null;
    }

    return {
      street: data.logradouro || "",
      neighborhood: data.bairro || "",
      city: data.localidade || "",
      state: data.uf || "",
      complement: data.complemento || "",
    };
  } catch (err) {
    console.error("Erro ao buscar CEP no ViaCEP:", err);
    return null;
  }
}
