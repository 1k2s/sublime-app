import { KeyRound, Mail, ShieldCheck, ArrowLeft, ArrowRight } from "lucide-react";
import { FormField } from "@/shared/ui/FormField";
import { Input } from "@/shared/ui/Input";
import { Select } from "@/shared/ui/Select";
import { Button } from "@/shared/ui/Button";
import styles from "./ProviderWizard.module.css";

export function ProviderAccessStep({
  register,
  errors,
  isEditing = false,
  onBack,
  onNext,
}) {
  return (
    <div className={styles.stepContainer}>
      <div className={styles.cardHeader}>
        <div className={styles.headerIconCircle}>
          <KeyRound size={22} />
        </div>
        <div>
          <h2 className={styles.cardTitle}>Conta de acesso</h2>
          <p className={styles.cardSubtitle}>
            Configure o e-mail de login, o perfil e a senha provisória de primeiro
            acesso.
          </p>
        </div>
      </div>

      <div className={styles.formFields}>
        <div className={styles.row}>
          <FormField
            label={
              <>
                E-mail de acesso <span className={styles.required}>*</span>
              </>
            }
            htmlFor="provider-email"
            error={errors.email?.message}
          >
            <Input
              id="provider-email"
              icon={Mail}
              type="email"
              placeholder="beatriz@sublime.com"
              invalid={Boolean(errors.email)}
              {...register("email")}
            />
          </FormField>

          <FormField
            label={
              <>
                Perfil de acesso <span className={styles.required}>*</span>
              </>
            }
            htmlFor="provider-role"
            error={errors.role?.message}
          >
            <Select
              id="provider-role"
              invalid={Boolean(errors.role)}
              {...register("role")}
            >
              <option value="PROVIDER">Prestador</option>
              <option value="ADMIN">Administrador (também atende)</option>
            </Select>
          </FormField>
        </div>

        <div className={isEditing ? styles.row : undefined}>
          <FormField
            label={
              isEditing ? (
                "Nova senha provisória (opcional)"
              ) : (
                <>
                  Senha provisória de primeiro acesso{" "}
                  <span className={styles.required}>*</span>
                </>
              )
            }
            htmlFor="provider-password"
            error={errors.password?.message}
          >
            <Input
              id="provider-password"
              type="password"
              autoComplete="new-password"
              placeholder={
                isEditing
                  ? "Em branco mantém a senha atual"
                  : "Mínimo de 8 caracteres"
              }
              invalid={Boolean(errors.password)}
              {...register("password")}
            />
          </FormField>

          {isEditing && (
            <FormField label="Status do prestador" htmlFor="provider-status">
              <Select id="provider-status" {...register("status")}>
                <option value="ACTIVE">Ativo</option>
                <option value="INACTIVE">Inativo</option>
              </Select>
            </FormField>
          )}
        </div>

        <div className={styles.helperBanner}>
          <ShieldCheck size={18} className={styles.helperIcon} />
          <p className={styles.helperText}>
            <strong>Primeiro acesso seguro (LGPD):</strong> ao entrar pela primeira
            vez com esta senha provisória, o prestador será direcionado
            automaticamente para definir sua própria senha pessoal.
          </p>
        </div>
      </div>

      <div className={styles.cardFooter}>
        <Button variant="outline" type="button" onClick={onBack}>
          <ArrowLeft size={18} /> Voltar
        </Button>
        <Button type="button" onClick={onNext}>
          Próximo <ArrowRight size={18} />
        </Button>
      </div>
    </div>
  );
}
