// O prestador por quem o ADMIN lança, lembrado neste navegador (o admin costuma lançar vários de
// um mesmo prestador). Fica fora do useLaunchDraft porque o resumo do dia, de onde vem o "hoje"
// que o draft usa, já depende dele. Para o PROVIDER não se aplica (é sempre ele mesmo)
import { useState } from "react";

const STORAGE_KEY = "sublime.launchProviderId";

function readStored() {
  try {
    return localStorage.getItem(STORAGE_KEY) ?? "";
  } catch {
    return "";
  }
}

export function useLaunchProvider(isAdmin) {
  const [providerId, setProviderIdState] = useState(() => (isAdmin ? readStored() : ""));

  function setProviderId(value) {
    setProviderIdState(value);
    try {
      localStorage.setItem(STORAGE_KEY, value);
    } catch {
      // Sem acesso ao armazenamento (ex: bloqueado pelo navegador): só não lembra
    }
  }

  return [providerId, setProviderId];
}
