import Swal from "sweetalert2";

export function showError(message) {
  return Swal.fire({
    icon: "error",
    title: "Algo deu errado",
    text: message,
    confirmButtonText: "Ok",
    confirmButtonColor: "#5f7a58",
  });
}

// Aviso que não impede nada (a ação já foi feita), só chama a atenção do usuário
export function showWarning(message) {
  return Swal.fire({
    icon: "warning",
    title: "Atenção",
// Confirmação de que a ação deu certo, para telas que não fecham nem navegam depois de salvar
export function showSuccess(message) {
  return Swal.fire({
    icon: "success",
    title: "Tudo certo",
    text: message,
    confirmButtonText: "Ok",
    confirmButtonColor: "#5f7a58",
  });
}
