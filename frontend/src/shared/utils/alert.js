import Swal from "sweetalert2";

export function showError(message) {
  return Swal.fire({
    icon: "error",
    title: "Algo deu errado",
    text: message,
    confirmButtonText: "Ok",
    confirmButtonColor: "var(--color-primary)",
  });
}

// Confirmação de que a ação deu certo, para telas que não fecham nem navegam depois de salvar
export function showSuccess(message) {
  return Swal.fire({
    icon: "success",
    title: "Tudo certo",
    text: message,
    confirmButtonText: "Ok",
    confirmButtonColor: "var(--color-primary)",
  });
}
