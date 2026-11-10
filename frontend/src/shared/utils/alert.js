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
    text: message,
    confirmButtonText: "Ok",
    confirmButtonColor: "#5f7a58",
  });
}
