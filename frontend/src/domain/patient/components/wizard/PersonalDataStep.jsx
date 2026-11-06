import { User, Calendar, Phone, Mail, ArrowRight } from "lucide-react";
import { FormField } from "@/shared/ui/FormField";
import { Input } from "@/shared/ui/Input";
import { Button } from "@/shared/ui/Button";
import { maskCpf, maskPhone, maskDate } from "@/shared/utils/masks";
import styles from "./PatientWizard.module.css";

export function PersonalDataStep({
  register,
  errors,
  setValue,
  watch,
  onCancel,
  onNext,
}) {
  const cpfValue = watch("cpf");
  const phoneValue = watch("phone");
  const birthDateValue = watch("birthDate");

  const handleCpfChange = (e) => {
    const masked = maskCpf(e.target.value);
    setValue("cpf", masked, { shouldValidate: true });
  };

  const handlePhoneChange = (e) => {
    const masked = maskPhone(e.target.value);
    setValue("phone", masked, { shouldValidate: true });
  };

  const handleBirthDateChange = (e) => {
    const masked = maskDate(e.target.value);
    setValue("birthDate", masked, { shouldValidate: true });
  };

  return (
    <div className={styles.stepContainer}>
      <div className={styles.cardHeader}>
        <div className={styles.headerIconCircle}>
          <User size={22} />
        </div>
        <div>
          <h2 className={styles.cardTitle}>Dados pessoais</h2>
          <p className={styles.cardSubtitle}>
            Informe os dados de identificação do paciente.
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
              placeholder="123.456.789-09"
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
