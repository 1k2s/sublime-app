import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { getRole } from "@/domain/auth/roles";
import { Button } from "@/shared/ui/Button";
import { FormField } from "@/shared/ui/FormField";
import { Input } from "@/shared/ui/Input";
import { Modal } from "@/shared/ui/Modal";
import { Select } from "@/shared/ui/Select";
import { createProviderSchema, updateProviderSchema } from "../schemas/providerSchema";
import { toPercentageInput, toPercentageRequest } from "../percentage";
import styles from "./ProviderFormModal.module.css";

// provider nulo = cadastro; preenchido = edição (o usuário vinculado não muda)
export function ProviderFormModal({ provider = null, users = [], isSubmitting = false, onSubmit, onClose }) {
  const isEditing = Boolean(provider);

  const {
    register,
    handleSubmit,
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

  // Entrega no formato do ProviderRequestDTO / ProviderUpdateRequestDTO
  function submit(values) {
    const payload = {
      name: values.name.trim(),
      commissionPercentage: toPercentageRequest(values.commissionPercentage),
      referralCommissionPercentage: toPercentageRequest(values.referralCommissionPercentage),
    };
    onSubmit?.(isEditing ? payload : { userId: Number(values.userId), ...payload });
  }

  const percentagePlaceholder = isEditing ? "Ex: 40" : "Padrão da clínica";

  return (
    <Modal title={isEditing ? "Editar prestador" : "Novo prestador"} onClose={onClose}>
      <form className={styles.form} onSubmit={handleSubmit(submit)} noValidate>
        {!isEditing && (
          <FormField label="Usuário de acesso" htmlFor="userId" error={errors.userId?.message}>
            <Select id="userId" invalid={Boolean(errors.userId)} {...register("userId")}>
              <option value="">Selecione</option>
              {users.map((user) => (
                <option key={user.id} value={user.id}>
                  {user.email} ({getRole(user.role)?.label ?? user.role})
                </option>
              ))}
            </Select>
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
            label="Pacientes que trouxe (%)"
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

        {!isEditing && (
          <p className={styles.hint}>Deixe os percentuais em branco para usar o padrão da clínica.</p>
        )}

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
