import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { ROLES } from "@/domain/auth/roles";
import { Button } from "@/shared/ui/Button";
import { FormField } from "@/shared/ui/FormField";
import { Input } from "@/shared/ui/Input";
import { Modal } from "@/shared/ui/Modal";
import { Select } from "@/shared/ui/Select";
import { createUserSchema, updateUserSchema } from "../schemas/userSchema";
import styles from "./UserFormModal.module.css";

// user nulo = cadastro; preenchido = edição
export function UserFormModal({ user = null, isSubmitting = false, onSubmit, onClose }) {
  const isEditing = Boolean(user);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(isEditing ? updateUserSchema : createUserSchema),
    defaultValues: {
      email: user?.email ?? "",
      role: user?.role ?? "",
      password: "",
    },
  });

  // Entrega no formato do UserRequestDTO / UserUpdateRequestDTO (senha nula = manter a atual)
  function submit(values) {
    onSubmit?.({
      email: values.email.trim(),
      role: values.role,
      password: values.password === "" ? null : values.password,
    });
  }

  return (
    <Modal title={isEditing ? "Editar usuário" : "Novo usuário"} onClose={onClose}>
      <form className={styles.form} onSubmit={handleSubmit(submit)} noValidate>
        <FormField label="E-mail" htmlFor="email" error={errors.email?.message}>
          <Input
            id="email"
            type="email"
            autoComplete="off"
            placeholder="nome@sublime.com.br"
            invalid={Boolean(errors.email)}
            {...register("email")}
          />
        </FormField>

        <FormField label="Perfil" htmlFor="role" error={errors.role?.message}>
          <Select id="role" invalid={Boolean(errors.role)} {...register("role")}>
            <option value="">Selecione</option>
            {Object.values(ROLES).map((role) => (
              <option key={role.value} value={role.value}>
                {role.label}
              </option>
            ))}
          </Select>
        </FormField>

        <FormField label={isEditing ? "Nova senha" : "Senha"} htmlFor="password" error={errors.password?.message}>
          <Input
            id="password"
            type="password"
            autoComplete="new-password"
            placeholder={isEditing ? "Deixe em branco para manter a atual" : "Mínimo de 8 caracteres"}
            invalid={Boolean(errors.password)}
            {...register("password")}
          />
        </FormField>

        {isEditing && (
          <p className={styles.hint}>Mudar o perfil encerra a sessão atual desse usuário.</p>
        )}

        <div className={styles.actions}>
          <Button variant="outline" onClick={onClose} disabled={isSubmitting}>
            Cancelar
          </Button>
          <Button type="submit" loading={isSubmitting}>
            {isEditing ? "Salvar alterações" : "Cadastrar usuário"}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
