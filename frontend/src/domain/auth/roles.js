export const ROLES = {
  ADMIN: { value: "admin", label: "Administrador", homePath: "/patients" },
  PROVIDER: { value: "provider", label: "Prestador", homePath: "/consultations" },
};

export function getRole(value) {
  return Object.values(ROLES).find((role) => role.value === value);
}

export function getHomePath(value) {
  return getRole(value)?.homePath ?? "/login";
}
