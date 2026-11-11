import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { getRole, ROLES } from "@/domain/auth/roles";
import { Button } from "@/shared/components/Button";
import { FormField } from "@/shared/components/FormField";
import { Input } from "@/shared/components/Input";
import { Modal } from "@/shared/components/Modal";
import { Select } from "@/shared/components/Select";
import { createUserSchema, updateUserSchema } from "../schemas/userSchema";
import styles from "./UserFormModal.module.css";

// user nulo = cadastro; preenchido = edição
export function UserFormModal({ user = null, isSubmitting = false, onSubmit, onClose }) {
  const isEditing = Boolean(user);
  // O perfil do admin padrão não muda (a API responde 409): o campo fica só para leitura.
  // Ver "Admin padrão" em User, no domain-model
  const isRoleLocked = Boolean(user?.defaultAdmin);

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
          {isRoleLocked ? (
            // Fora do register: o valor enviado é o do defaultValues, que não muda
            <Input id="role" value={getRole(user.role)?.label ?? user.role} readOnly disabled />
          ) : (
            <Select id="role" invalid={Boolean(errors.role)} {...register("role")}>
              <option value="">Selecione</option>
              {Object.values(ROLES).map((role) => (
                <option key={role.value} value={role.value}>
                  {role.label}
                </option>
              ))}
            </Select>
          )}
        </FormField>

        <FormField
          label={isEditing ? "Nova senha provisória" : "Senha provisória"}
          htmlFor="password"
          error={errors.password?.message}
        >
          <Input
            id="password"
            type="password"
            autoComplete="new-password"
            placeholder={isEditing ? "Deixe em branco para manter a atual" : "Mínimo de 8 caracteres"}
            invalid={Boolean(errors.password)}
            {...register("password")}
          />
        </FormField>

        <p className={styles.hint}>
          {isEditing
            ? "Preencher a senha a redefine: o usuário cria uma nova no próximo login. Redefinir a senha ou mudar o perfil encerra a sessão aberta desse usuário."
            : "No primeiro login, o usuário troca a senha provisória por uma senha pessoal."}
        </p>

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
