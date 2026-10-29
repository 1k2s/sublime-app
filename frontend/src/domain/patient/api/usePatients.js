import { useQuery } from "@tanstack/react-query";
import { listPatients } from "./patientApi";

export function usePatients(search, { enabled = true } = {}) {
  return useQuery({
    queryKey: ["patients", search],
    queryFn: () => listPatients(search),
    enabled,
  });
}
