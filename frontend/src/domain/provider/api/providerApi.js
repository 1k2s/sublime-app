import { apiClient } from "@/shared/http/apiClient";
import { createUser, listUsers, updateUser } from "@/domain/user/api/userApi";

// Formata percentual numérico vindo da API (ex: 40 ou 40.00 -> "40" ou "40.5")
function formatPercentageInput(value, fallback) {
  if (value === null || value === undefined || value === "") return fallback;
  const num = Number(value);
  if (Number.isNaN(num)) return fallback;
  return Number.isInteger(num) ? String(num) : String(num);
}

function mapProviderViewModel(provider, userMap) {
  const linkedUser = userMap.get(provider.userId);
  return {
    id: provider.id,
    userId: provider.userId,
    name: provider.name,
    commissionPercentage: formatPercentageInput(provider.commissionPercentage, "40"),
    referralCommissionPercentage: formatPercentageInput(provider.referralCommissionPercentage, "45"),
    active: Boolean(provider.active),
    status: provider.active ? "ACTIVE" : "INACTIVE",
    email: linkedUser?.email || "",
    role: linkedUser?.role || "PROVIDER",
    userActive: linkedUser ? Boolean(linkedUser.active) : true,
  };
}

// GET /api/providers
export async function listProviders() {
  const { data } = await apiClient.get("/providers");
  return data;
}

// Busca prestadores combinados com os dados da conta de usuário vinculada
export async function listProvidersWithUsers() {
  const [providers, users] = await Promise.all([listProviders(), listUsers()]);
  const userMap = new Map(users.map((u) => [u.id, u]));
  return providers.map((provider) => mapProviderViewModel(provider, userMap));
}

// POST /api/providers
export async function createProvider({
  userId,
  name,
  commissionPercentage,
  referralCommissionPercentage,
}) {
  const { data } = await apiClient.post("/providers", {
    userId,
    name,
    commissionPercentage: Number(String(commissionPercentage).replace(",", ".")),
    referralCommissionPercentage: Number(String(referralCommissionPercentage).replace(",", ".")),
  });
  return data;
}

// PUT /api/providers/{id}
export async function updateProvider(
  id,
  { name, commissionPercentage, referralCommissionPercentage }
) {
  const { data } = await apiClient.put(`/providers/${id}`, {
    name,
    commissionPercentage: Number(String(commissionPercentage).replace(",", ".")),
    referralCommissionPercentage: Number(String(referralCommissionPercentage).replace(",", ".")),
  });
  return data;
}

// DELETE /api/providers/{id} (soft delete)
export async function deactivateProvider(id) {
  await apiClient.delete(`/providers/${id}`);
}

// PATCH /api/providers/{id}/activate
export async function activateProvider(id) {
  await apiClient.patch(`/providers/${id}/activate`);
}

// Fluxo completo de criação no Wizard: cria o User (com senha provisória) e em seguida o Provider
export async function createProviderWithUser(formData) {
  const createdUser = await createUser({
    email: formData.email.trim(),
    password: formData.password,
    role: formData.role,
  });

  const createdProvider = await createProvider({
    userId: createdUser.id,
    name: formData.name.trim(),
    commissionPercentage: formData.commissionPercentage,
    referralCommissionPercentage: formData.referralCommissionPercentage,
  });

  return mapProviderViewModel(createdProvider, new Map([[createdUser.id, createdUser]]));
}

// Fluxo completo de edição no Wizard: atualiza User, atualiza Provider e sincroniza status
export async function updateProviderWithUser({ id, currentProvider, data }) {
  const updatedUser = await updateUser(currentProvider.userId, {
    email: data.email.trim(),
    role: data.role,
    password: data.password,
  });

  let updatedProvider = await updateProvider(id, {
    name: data.name.trim(),
    commissionPercentage: data.commissionPercentage,
    referralCommissionPercentage: data.referralCommissionPercentage,
  });

  const currentStatus = currentProvider.status || (currentProvider.active ? "ACTIVE" : "INACTIVE");
  const nextStatus = data.status || currentStatus;

  if (nextStatus !== currentStatus) {
    if (nextStatus === "INACTIVE") {
      await deactivateProvider(id);
      updatedProvider = { ...updatedProvider, active: false };
    } else if (nextStatus === "ACTIVE") {
      await activateProvider(id);
      updatedProvider = { ...updatedProvider, active: true };
    }
  }

  return mapProviderViewModel(updatedProvider, new Map([[updatedUser.id, updatedUser]]));
}
