export const ROLES = {
  // "value" idêntico ao enum Role do backend (vem no login, em user.role)
  ADMIN: { value: "ADMIN", label: "Administrador", homePath: "/patients" },
  PROVIDER: { value: "PROVIDER", label: "Prestador", homePath: "/consultations" },
};

export function getRole(value) {
  return Object.values(ROLES).find((role) => role.value === value);
}

export function getHomePath(value) {
  return getRole(value)?.homePath ?? "/login";
}
