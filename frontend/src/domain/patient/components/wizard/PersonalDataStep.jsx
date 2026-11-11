// Etapa 1 do cadastro de paciente: dados pessoais, contato (opcional) e origem do paciente
import { useWatch } from "react-hook-form";
import { ArrowRight, Calendar, Mail, Phone, User } from "lucide-react";
import { Button } from "@/shared/components/Button";
import { FormField } from "@/shared/components/FormField";
import { Input } from "@/shared/components/Input";
import { Select } from "@/shared/components/Select";
import { maskCpf, maskDate, maskPhone } from "@/shared/utils/masks";
import styles from "@/domain/patient/components/wizard/PatientWizard.module.css";

export function PersonalDataStep({
  register,
  control,
  errors,
  setValue,
  providers = [],
  onCancel,
  onNext,
}) {
  const cpfValue = useWatch({ control, name: "cpf" });
  const phoneValue = useWatch({ control, name: "phone" });
  const birthDateValue = useWatch({ control, name: "birthDate" });

  function handleCpfChange(e) {
    setValue("cpf", maskCpf(e.target.value), { shouldValidate: true });
  }

  function handlePhoneChange(e) {
    setValue("phone", maskPhone(e.target.value), { shouldValidate: true });
  }

  function handleBirthDateChange(e) {
    setValue("birthDate", maskDate(e.target.value), { shouldValidate: true });
  }

  return (
    <div className={styles.stepContainer}>
      <div className={styles.cardHeader}>
        <div className={styles.headerIconCircle}>
          <User size={22} />
        </div>
        <div>
          <h2 className={styles.cardTitle}>Dados pessoais</h2>
          <p className={styles.cardSubtitle}>
            Informe os dados de identificação, contato e origem do paciente.
          </p>
        </div>
      </div>

      <div className={styles.formFields}>
        <FormField
          label={
            <>
              Nome completo <span className={styles.required}>*</span>
            </>
          }
          htmlFor="name"
          error={errors.name?.message}
        >
          <Input
            id="name"
            icon={User}
            placeholder="Maria Eduarda Santos"
            invalid={Boolean(errors.name)}
            {...register("name")}
          />
        </FormField>

        <div className={styles.row}>
          <FormField
            label={
              <>
                CPF <span className={styles.required}>*</span>
              </>
            }
            htmlFor="cpf"
            error={errors.cpf?.message}
          >
            <Input
              id="cpf"
              placeholder="123.456.789-00"
              value={cpfValue || ""}
              onChange={handleCpfChange}
              invalid={Boolean(errors.cpf)}
            />
          </FormField>

          <FormField
            label={
              <>
                Data de nascimento <span className={styles.required}>*</span>
              </>
            }
            htmlFor="birthDate"
            error={errors.birthDate?.message}
          >
            <Input
              id="birthDate"
              icon={Calendar}
              placeholder="14/03/1990"
              value={birthDateValue || ""}
              onChange={handleBirthDateChange}
              invalid={Boolean(errors.birthDate)}
            />
          </FormField>
        </div>

        <div className={styles.row}>
          <FormField
            label="Telefone"
            htmlFor="phone"
            error={errors.phone?.message}
          >
            <Input
              id="phone"
              icon={Phone}
              placeholder="(16) 99123-4567"
              value={phoneValue || ""}
              onChange={handlePhoneChange}
              invalid={Boolean(errors.phone)}
            />
          </FormField>

          <FormField
            label="E-mail"
            htmlFor="email"
            error={errors.email?.message}
          >
            <Input
              id="email"
              icon={Mail}
              type="email"
              placeholder="maria.santos@email.com"
              invalid={Boolean(errors.email)}
              {...register("email")}
            />
          </FormField>
        </div>

        <FormField
          label="Origem do paciente (prestador que trouxe)"
          htmlFor="referringProviderId"
          error={errors.referringProviderId?.message}
        >
          <Select
            id="referringProviderId"
            invalid={Boolean(errors.referringProviderId)}
            {...register("referringProviderId")}
          >
            <option value="">Clínica (sem prestador)</option>
            {providers.map((provider) => (
              <option key={provider.id} value={String(provider.id)}>
                {provider.name}
                {!provider.active ? " (Inativo)" : ""}
              </option>
            ))}
          </Select>
        </FormField>
      </div>

      <div className={styles.cardFooter}>
        <Button variant="outline" type="button" onClick={onCancel}>
          Cancelar
        </Button>
        <Button type="button" onClick={onNext}>
          Próximo <ArrowRight size={18} />
        </Button>
      </div>
    </div>
  );
}
