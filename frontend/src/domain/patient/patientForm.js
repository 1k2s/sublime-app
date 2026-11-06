// Conversão entre o paciente da API (PatientResponseDTO / PatientRequestDTO) e o formulário.
// A API trafega CPF, CEP e telefone só com dígitos e a data em ISO; a tela usa máscaras.
import { formatDate } from "@/shared/utils/date";
import { maskCep, maskCpf, maskPhone, onlyDigits } from "@/shared/utils/masks";

// Campos do endereço no formulário (mesmos nomes do AddressDTO)
export const ADDRESS_FIELDS = ["cep", "street", "numberHouse", "complement", "city"];

// "14/03/1990" -> "1990-03-14"
function toIsoDate(maskedDate) {
  const [day, month, year] = maskedDate.split("/");
  return `${year}-${month}-${day}`;
}

function emptyToNull(value) {
  const trimmed = value?.trim() ?? "";
  return trimmed === "" ? null : trimmed;
}

// O endereço é opcional: todos os campos vazios = sem endereço
export function hasAddress(values) {
  return ADDRESS_FIELDS.some((field) => values[field]?.trim());
}

// Paciente da API (ou null, no cadastro) -> valores iniciais do formulário
export function toPatientFormValues(patient) {
  const address = patient?.address;
  return {
    name: patient?.name ?? "",
    cpf: maskCpf(patient?.cpf ?? ""),
    birthDate: formatDate(patient?.birthDate),
    phone: maskPhone(patient?.phone ?? ""),
    email: patient?.email ?? "",
    cep: maskCep(address?.cep ?? ""),
    street: address?.street ?? "",
    numberHouse: address?.numberHouse ?? "",
    complement: address?.complement ?? "",
    city: address?.city ?? "",
  };
}

// Valores do formulário -> PatientRequestDTO. A origem não está no formulário: na edição,
// a página reenvia a atual (nula no PUT tornaria o paciente "da clínica")
export function toPatientRequest(values, referringProviderId = null) {
  return {
    name: values.name.trim(),
    cpf: onlyDigits(values.cpf),
    birthDate: toIsoDate(values.birthDate),
    phone: emptyToNull(onlyDigits(values.phone)),
    email: emptyToNull(values.email),
    address: hasAddress(values)
      ? {
          street: values.street.trim(),
          numberHouse: values.numberHouse.trim(),
          city: values.city.trim(),
          complement: emptyToNull(values.complement),
          cep: onlyDigits(values.cep),
        }
      : null,
    referringProviderId,
  };
}
