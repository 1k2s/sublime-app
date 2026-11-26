// Texto para comparar em buscas: sem acentos, em minúsculas e sem espaços nas pontas
// ("Quiropraxia" e "quiro" combinam; "Pilates em Grupo" e "pilates" também)
export function normalizeText(value) {
  return String(value ?? "")
    .normalize("NFD")
    .replace(/\p{Diacritic}/gu, "")
    .toLowerCase()
    .trim();
}
