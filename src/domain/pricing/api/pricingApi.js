// Chamadas HTTP do módulo pricing (uma função por endpoint)

// Mock até o backend expor /techniques. Depois, trocar o corpo por httpClient.get("/techniques")
const MOCK_TECHNIQUES = [
  { id: 1, name: "Pilates" },
  { id: 2, name: "RPG" },
  { id: 3, name: "Fisioterapia ortopédica" },
  { id: 4, name: "Drenagem linfática" },
  { id: 5, name: "Acupuntura" },
];

export async function listTechniques() {
  await new Promise((resolve) => setTimeout(resolve, 200));
  return MOCK_TECHNIQUES;
}
