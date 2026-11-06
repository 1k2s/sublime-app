// Formatação de datas (chegam da API em ISO 8601)
import { format, isValid, parse, parseISO } from "date-fns";
import { ptBR } from "date-fns/locale";

function toDate(value) {
  if (!value) return null;
  const date = value instanceof Date ? value : parseISO(value);
  return isValid(date) ? date : null;
}

// "2026-10-06" -> "06/10/2026". Devolve "" para valor vazio ou inválido
export function formatDate(value, pattern = "dd/MM/yyyy") {
  const date = toDate(value);
  return date ? format(date, pattern, { locale: ptBR }) : "";
}

// "14/03/1990" -> Date válida (ou null se a data não existir no calendário)
export function parseDisplayDate(value) {
  if (!value || typeof value !== "string" || value.trim().length !== 10) return null;
  const parsed = parse(value.trim(), "dd/MM/yyyy", new Date());
  return isValid(parsed) && format(parsed, "dd/MM/yyyy") === value.trim() ? parsed : null;
}

// "14/03/1990" -> "1990-03-14" (formato LocalDate da API)
export function displayDateToIso(value) {
  const date = parseDisplayDate(value);
  return date ? format(date, "yyyy-MM-dd") : "";
}

// "2026-10-06T14:30:00" -> "06/10/2026 14:30"
export function formatDateTime(value) {
  return formatDate(value, "dd/MM/yyyy HH:mm");
}

// Date -> "2026-10-06T14:30", formato do <input type="datetime-local">
export function toDateTimeInputValue(date) {
  return format(date, "yyyy-MM-dd'T'HH:mm");
}

// Date -> "2026-10-06", formato de data da API e do <input type="date">
export function toDateInputValue(date) {
  return format(date, "yyyy-MM-dd");
}
