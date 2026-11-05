/**
 * ======================================================================================
 * CAMADA DE API: MÓDULO PATIENT (PACIENTES)
 * ======================================================================================
 *
 * 📌 GUIA DE INTEGRAÇÃO COM O BACKEND REAL (PARA O PRÓXIMO DESENVOLVEDOR OU IA):
 * --------------------------------------------------------------------------------------
 * Atualmente, este arquivo utiliza uma implementação simulada (MOCK) persistida no
 * `localStorage` do navegador para permitir o desenvolvimento da interface e testes de
 * ponta a ponta sem dependência de o backend Spring Boot estar rodando.
 *
 * 🚀 PASSO A PASSO PARA CONECTAR AO BACKEND REAL:
 *
 * 1. Descomente a importação do `httpClient`:
 *      import { httpClient } from "@/shared/http/httpClient";
 *
 * 2. Apague ou comente a constante `STORAGE_KEY`, o array `INITIAL_PATIENTS` e as funções
 *    auxiliares `getStoredPatients()` e `saveStoredPatients()`.
 *
 * 3. Substitua as funções mockadas abaixo pelas chamadas reais ao backend:
 *
 *    // Listar pacientes com filtros de busca e status:
 *    export async function listPatients({ search = "", status = "ALL" } = {}) {
 *      const params = {};
 *      if (search) params.search = search;
 *      if (status && status !== "ALL") params.status = status;
 *      const { data } = await httpClient.get("/patients", { params });
 *      return data;
 *    }
 *
 *    // Buscar paciente por ID:
 *    export async function getPatientById(id) {
 *      const { data } = await httpClient.get(`/patients/${id}`);
 *      return data;
 *    }
 *
 *    // Cadastrar novo paciente:
 *    export async function createPatient(payload) {
 *      const { data } = await httpClient.post("/patients", payload);
 *      return data;
 *    }
 *
 *    // Atualizar paciente existente:
 *    export async function updatePatient(id, payload) {
 *      const { data } = await httpClient.put(`/patients/${id}`, payload);
 *      return data;
 *    }
 *
 * 4. Verifique o contrato da API no Swagger/OpenAPI do Spring Boot (`/swagger-ui.html`)
 *    para alinhar os nomes exatos dos campos do DTO se houver alguma divergência.
 *
 * ======================================================================================
 */

import { onlyDigits } from "@/shared/utils/masks";
// import { httpClient } from "@/shared/http/httpClient"; // <- Descomente quando integrar com a API real

/**
 * Chave de armazenamento no localStorage para simular a base de dados em ambiente local.
 */
const STORAGE_KEY = "sublime.patients";

/**
 * Dados iniciais padrão para povoar a listagem na primeira execução,
 * correspondendo exatamente aos registros apresentados no design de referência.
 */
const INITIAL_PATIENTS = [
  {
    id: "1",
    name: "Maria Eduarda Santos",
    cpf: "123.456.789-09",
    phone: "(16) 99123-4567",
    birthDate: "14/03/1990",
    email: "maria.santos@email.com",
    city: "São Carlos",
    state: "SP",
    cep: "13560-049",
    street: "Rua Episcopal",
    number: "123",
    complement: "Apto 42",
    neighborhood: "Centro",
    status: "ACTIVE",
  },
  {
    id: "2",
    name: "João Pedro Almeida",
    cpf: "234.567.890-92",
    phone: "(16) 99234-5678",
    birthDate: "22/07/1985",
    email: "joao.almeida@email.com",
    city: "São Carlos",
    state: "SP",
    cep: "13560-011",
    street: "Av. São Carlos",
    number: "456",
    complement: "",
    neighborhood: "Centro",
    status: "ACTIVE",
  },
  {
    id: "3",
    name: "Fernanda Costa Lima",
    cpf: "345.678.901-75",
    phone: "(16) 99345-6789",
    birthDate: "05/11/1992",
    email: "fernanda.lima@email.com",
    city: "Ibaté",
    state: "SP",
    cep: "14815-000",
    street: "Rua Paulino Carlos",
    number: "78",
    complement: "",
    neighborhood: "Centro",
    status: "ACTIVE",
  },
  {
    id: "4",
    name: "Roberto Carlos Silva",
    cpf: "456.789.012-49",
    phone: "(16) 99456-7890",
    birthDate: "19/01/1978",
    email: "roberto.silva@email.com",
    city: "São Carlos",
    state: "SP",
    cep: "13560-042",
    street: "Rua Nove de Julho",
    number: "890",
    complement: "",
    neighborhood: "Centro",
    status: "INACTIVE",
  },
  {
    id: "5",
    name: "Ana Beatriz Oliveira",
    cpf: "567.890.123-03",
    phone: "(16) 99567-8901",
    birthDate: "30/09/1995",
    email: "ana.oliveira@email.com",
    city: "Araraquara",
    state: "SP",
    cep: "14801-320",
    street: "Rua Voluntários da Pátria",
    number: "1020",
    complement: "",
    neighborhood: "Centro",
    status: "ACTIVE",
  },
];

/**
 * Lê os pacientes persistidos no localStorage. Se ainda não existirem,
 * inicializa a base local com os pacientes padrão definidos acima.
 * @returns {Array<Object>} Lista de pacientes
 */
function getStoredPatients() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(INITIAL_PATIENTS));
      return INITIAL_PATIENTS;
    }
    const parsed = JSON.parse(raw);
    // Se a base contiver os CPFs antigos mockados com final "-00", sincroniza com os válidos
    if (parsed.length > 0 && parsed[0].cpf === "123.456.789-00") {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(INITIAL_PATIENTS));
      return INITIAL_PATIENTS;
    }
    return parsed;
  } catch {
    return INITIAL_PATIENTS;
  }
}

/**
 * Salva a lista atualizada de pacientes no localStorage do navegador.
 * @param {Array<Object>} patients - Lista de pacientes a persistir
 */
function saveStoredPatients(patients) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(patients));
  } catch (err) {
    console.error("Erro ao salvar paciente no localStorage:", err);
  }
}

/**
 * Retorna a listagem de pacientes aplicando filtros opcionais de busca textual e status.
 *
 * @param {Object} params - Parâmetros da consulta
 * @param {string} [params.search] - Termo de busca (nome ou dígitos do CPF)
 * @param {string} [params.status] - Filtro de status: "ALL", "ACTIVE" ou "INACTIVE"
 * @returns {Promise<Array<Object>>} Lista de pacientes filtrados
 */
export async function listPatients({ search = "", status = "ALL" } = {}) {
  // Simula latência de rede assíncrona (200ms)
  await new Promise((resolve) => setTimeout(resolve, 200));

  let list = getStoredPatients();

  // Filtra por status se não for "ALL"
  if (status && status !== "ALL") {
    list = list.filter((p) => p.status === status);
  }

  // Filtra por nome completo ou pelos dígitos do CPF
  if (search && search.trim()) {
    const term = search.trim().toLowerCase();
    const digits = onlyDigits(term);

    list = list.filter((p) => {
      const matchName = p.name.toLowerCase().includes(term);
      const matchCpf = digits ? onlyDigits(p.cpf).includes(digits) : false;
      return matchName || matchCpf;
    });
  }

  return list;
}

/**
 * Obtém os detalhes completos de um paciente a partir do seu identificador único.
 *
 * @param {string|number} id - Identificador do paciente
 * @returns {Promise<Object>} Dados do paciente localizado
 * @throws {Error} Se o paciente não for encontrado
 */
export async function getPatientById(id) {
  // Simula latência de rede assíncrona (150ms)
  await new Promise((resolve) => setTimeout(resolve, 150));
  const list = getStoredPatients();
  const patient = list.find((p) => String(p.id) === String(id));
  if (!patient) {
    throw new Error("Paciente não encontrado");
  }
  return patient;
}

/**
 * Cadastra um novo paciente na base de dados.
 *
 * @param {Object} payload - Dados cadastrais do novo paciente
 * @returns {Promise<Object>} Paciente cadastrado com ID gerado
 */
export async function createPatient(payload) {
  // Simula latência de rede assíncrona (300ms)
  await new Promise((resolve) => setTimeout(resolve, 300));
  const list = getStoredPatients();

  const newPatient = {
    ...payload,
    id: String(Date.now()), // Gera um ID único baseado em timestamp
    status: payload.status || "ACTIVE",
  };

  const updated = [newPatient, ...list];
  saveStoredPatients(updated);
  return newPatient;
}

/**
 * Atualiza os dados de um paciente existente.
 *
 * @param {string|number} id - Identificador do paciente a atualizar
 * @param {Object} payload - Novos dados a serem mesclados
 * @returns {Promise<Object>} Paciente com dados atualizados
 * @throws {Error} Se o paciente não for encontrado
 */
export async function updatePatient(id, payload) {
  // Simula latência de rede assíncrona (300ms)
  await new Promise((resolve) => setTimeout(resolve, 300));
  const list = getStoredPatients();

  const index = list.findIndex((p) => String(p.id) === String(id));
  if (index === -1) {
    throw new Error("Paciente não encontrado para atualização");
  }

  const updatedPatient = {
    ...list[index],
    ...payload,
    id: String(id),
  };

  list[index] = updatedPatient;
  saveStoredPatients([...list]);
  return updatedPatient;
}
