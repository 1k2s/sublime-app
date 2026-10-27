// Chamadas HTTP do módulo patient (uma função por endpoint)
import { onlyDigits } from "@/shared/utils/masks";

// Mock até o backend expor /patients. Depois, trocar o corpo por httpClient.get("/patients", { params: { search } })
const MOCK_PATIENTS = [
  { id: 1, name: "Ana Paula Souza", cpf: "12345678901", phone: "11987654321" },
  { id: 2, name: "Bruno Henrique Lima", cpf: "98765432100", phone: "11912345678" },
  { id: 3, name: "Carla Mendes", cpf: "45678912300", phone: "1132654321" },
  { id: 4, name: "Diego Albuquerque", cpf: "32165498700", phone: "21998877665" },
  { id: 5, name: "Fernanda Araújo", cpf: "74185296300", phone: "11955554444" },
];

// Ignora acentos e maiúsculas: "araujo" encontra "Araújo"
function normalize(text) {
  return text.normalize("NFD").replace(/[̀-ͯ]/g, "").toLowerCase();
}

// Busca por nome ou CPF (com ou sem máscara)
export async function listPatients(search) {
  await new Promise((resolve) => setTimeout(resolve, 300));

  const term = normalize(search.trim());
  const digits = onlyDigits(search);

  return MOCK_PATIENTS.filter(
    (patient) => normalize(patient.name).includes(term) || (digits.length >= 3 && patient.cpf.includes(digits))
  );
}
