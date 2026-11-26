// Texto para comparar em buscas: sem acentos, em minúsculas e sem espaços nas pontas
// ("Quiropraxia" e "quiro" combinam; "Pilates em Grupo" e "pilates" também)
export function normalizeText(value) {
  return String(value ?? "")
    .normalize("NFD")
    .replace(/\p{Diacritic}/gu, "")
    .toLowerCase()
    .trim();
}

// Iniciais do avatar: as do primeiro e do segundo nome ("Ana Souza" → "AS"; "Ana" → "AN")
export function getInitials(name) {
  const parts = String(name ?? "").trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return "--";
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return (parts[0][0] + parts[1][0]).toUpperCase();
}
