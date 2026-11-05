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
