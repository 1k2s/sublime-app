// Formatação de datas (chegam da API em ISO 8601)
import { format, isValid, parseISO } from "date-fns";
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
