import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { getRole } from "@/domain/auth/roles";
import { Button } from "@/shared/components/Button";
import { FormField } from "@/shared/components/FormField";
import { Input } from "@/shared/components/Input";
import { Modal } from "@/shared/components/Modal";
import { Select } from "@/shared/components/Select";
import { applyApiFieldErrors } from "@/shared/http/apiFieldErrors";
import { createProviderSchema, updateProviderSchema } from "../schemas/providerSchema";
import styles from "./ProviderFormModal.module.css";

// 40.5 -> "40,5", para preencher o campo na edição
function toPercentageInput(value) {
  return value == null ? "" : String(value).replace(".", ",");
}

// "40,5" -> "40.5"; vazio -> null (no cadastro, o backend aplica o padrão da clínica).
// Vai como texto, como o sessionValue dos preços: o backend lê o BigDecimal exato
function toPercentageRequest(value) {
  const trimmed = value.trim();
  return trimmed === "" ? null : trimmed.replace(",", ".");
}

function getUserLabel(user) {
  return `${user.email} (${getRole(user.role)?.label ?? user.role})`;
}

// provider nulo = cadastro; preenchido = edição (o usuário vinculado é fixo: só aparece).
// availableUsers: ativos e sem prestador (getAvailableUsers)
export function ProviderFormModal({ provider = null, availableUsers = [], isSubmitting = false, onSubmit, onClose }) {
  const isEditing = Boolean(provider);
  const percentagePlaceholder = isEditing ? "Ex: 40" : "Padrão da clínica";

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(isEditing ? updateProviderSchema : createProviderSchema),
    defaultValues: {
      userId: "",
      name: provider?.name ?? "",
      commissionPercentage: toPercentageInput(provider?.commissionPercentage),
      referralCommissionPercentage: toPercentageInput(provider?.referralCommissionPercentage),
    },
  });

  // Erro da API: a page chama no onError. Os campos têm os mesmos nomes do DTO
  function applyApiErrors(error) {
    applyApiFieldErrors(error, setError);
  }

  // Entrega no formato do ProviderRequestDTO / ProviderUpdateRequestDTO
  function submit(values) {
    const payload = {
      name: values.name.trim(),
      commissionPercentage: toPercentageRequest(values.commissionPercentage),
      referralCommissionPercentage: toPercentageRequest(values.referralCommissionPercentage),
    };
    onSubmit?.(isEditing ? payload : { userId: Number(values.userId), ...payload }, { applyApiErrors });
  }

  return (
    <Modal title={isEditing ? "Editar prestador" : "Novo prestador"} onClose={onClose}>
      <form className={styles.form} onSubmit={handleSubmit(submit)} noValidate>
        {isEditing ? (
          <FormField label="Usuário de acesso" htmlFor="userEmail">
            <Input id="userEmail" value={provider.email ?? "--"} readOnly disabled />
          </FormField>
        ) : (
          <FormField label="Usuário de acesso" htmlFor="userId" error={errors.userId?.message}>
            <Select id="userId" invalid={Boolean(errors.userId)} {...register("userId")}>
              <option value="">Selecione</option>
              {availableUsers.map((user) => (
                <option key={user.id} value={user.id}>
                  {getUserLabel(user)}
                </option>
              ))}
            </Select>
            {availableUsers.length === 0 && (
              <p className={styles.hint}>Nenhum usuário disponível. Cadastre o usuário na tela de Usuários.</p>
            )}
          </FormField>
        )}

        <FormField label="Nome" htmlFor="name" error={errors.name?.message}>
          <Input id="name" placeholder="Nome do prestador" invalid={Boolean(errors.name)} {...register("name")} />
        </FormField>

        <div className={styles.row}>
          <FormField
            label="Comissão padrão (%)"
            htmlFor="commissionPercentage"
            error={errors.commissionPercentage?.message}
          >
            <Input
              id="commissionPercentage"
              inputMode="decimal"
              placeholder={percentagePlaceholder}
              invalid={Boolean(errors.commissionPercentage)}
              {...register("commissionPercentage")}
            />
          </FormField>

          <FormField
            label="Comissão de pacientes que trouxe (%)"
            htmlFor="referralCommissionPercentage"
            error={errors.referralCommissionPercentage?.message}
          >
            <Input
              id="referralCommissionPercentage"
              inputMode="decimal"
              placeholder={percentagePlaceholder}
              invalid={Boolean(errors.referralCommissionPercentage)}
              {...register("referralCommissionPercentage")}
            />
          </FormField>
        </div>

        <p className={styles.hint}>
          {isEditing
            ? "Comissão padrão: pacientes da clínica ou trazidos por outro prestador. A dos pacientes que ele trouxe nunca é menor que a padrão."
            : "Deixe em branco para usar o padrão da clínica. A comissão dos pacientes que ele trouxe nunca é menor que a padrão."}
        </p>

        <div className={styles.actions}>
          <Button variant="outline" onClick={onClose} disabled={isSubmitting}>
            Cancelar
          </Button>
          <Button type="submit" loading={isSubmitting}>
            {isEditing ? "Salvar alterações" : "Cadastrar prestador"}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
