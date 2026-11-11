// Erros por campo de um 400 de validação (o "errors" do ProblemDetail: { campo: mensagem })
// marcados nos campos do formulário (React Hook Form). Campo de @AssertTrue chega com o nome do
// método sem "is" (ex: passwordValid), e campo aninhado com ponto (ex: address.cep): toFormField
// traduz o nome da API para o do formulário. Devolve os campos marcados (vazio = nenhum), para
// um wizard saber para qual etapa voltar. Ver "Formato dos erros" no architecture.md
export function applyApiFieldErrors(error, setError, toFormField = (apiField) => apiField) {
  const apiErrors = error?.response?.data?.errors;
  if (!apiErrors || typeof apiErrors !== "object") return [];

  return Object.entries(apiErrors).map(([apiField, message]) => {
    const field = toFormField(apiField);
    setError(field, { type: "server", message });
    return field;
  });
}
