import { useMutation } from "@tanstack/react-query";
import { login } from "./authApi";

export function useLogin() {
  return useMutation({ mutationFn: login });
}
