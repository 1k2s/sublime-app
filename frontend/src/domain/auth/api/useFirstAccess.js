import { useMutation } from "@tanstack/react-query";
import { completeFirstAccess } from "./authApi";

export function useFirstAccess() {
  return useMutation({ mutationFn: completeFirstAccess });
}
