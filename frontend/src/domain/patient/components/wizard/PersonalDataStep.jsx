/**
 * ======================================================================================
 * COMPONENTE: PersonalDataStep (Etapa 1 do Wizard)
 * ======================================================================================
 *
 * Apresenta o formulário de identificação do paciente (conforme Imagem 2):
 * - Nome completo (com ícone de usuário `User`).
 * - CPF (com máscara em tempo real `000.000.000-00`).
 * - Data de nascimento (com ícone `Calendar`, máscara automática `DD/MM/AAAA` e validação).
 * - Telefone (com ícone `Phone` e máscara `(00) 00000-0000`).
 * - E-mail (com ícone `Mail`, obrigatório).
 * - Botões: "Cancelar" (volta para a listagem) e "Próximo →" (valida e avança para o endereço).
 */

import { User, Calendar, Phone, Mail, ArrowRight } from "lucide-react";
import { FormField } from "@/shared/ui/FormField";
import { Input } from "@/shared/ui/Input";
import { Button } from "@/shared/ui/Button";
import { maskCpf, maskPhone, maskDate } from "@/shared/utils/masks";
import styles from "./PatientWizard.module.css";

/**
 * @param {Object} props
 * @param {import("react-hook-form").UseFormRegister} props.register - Função register do React Hook Form
 * @param {Object} props.errors - Objeto de erros de validação da etapa
 * @param {import("react-hook-form").UseFormSetValue} props.setValue - Função para alterar valores programaticamente
 * @param {import("react-hook-form").UseFormWatch} props.watch - Função para observar valores em tempo real
 * @param {function(): void} props.onCancel - Callback ao cancelar o fluxo
 * @param {function(): void} props.onNext - Callback para avançar para a próxima etapa (endereço)
 */
export function PersonalDataStep({
  register,
  errors,
  setValue,
  watch,
  onCancel,
  onNext,
}) {
  // Observa os campos que possuem máscara para controle em tempo real
  const cpfValue = watch("cpf");
  const phoneValue = watch("phone");
  const birthDateValue = watch("birthDate");

  // Aplica máscara de CPF durante a digitação e dispara validação
  const handleCpfChange = (e) => {
    const masked = maskCpf(e.target.value);
    setValue("cpf", masked, { shouldValidate: true });
  };

  // Aplica máscara de Telefone celular ou fixo durante a digitação
  const handlePhoneChange = (e) => {
    const masked = maskPhone(e.target.value);
    setValue("phone", masked, { shouldValidate: true });
  };

  // Aplica formatação automática de barras para Data de Nascimento (DD/MM/AAAA)
  const handleBirthDateChange = (e) => {
    const masked = maskDate(e.target.value);
    setValue("birthDate", masked, { shouldValidate: true });
  };

  return (
    <div className={styles.stepContainer}>
      {/* Cabeçalho do Card com ícone em círculo verde suave */}
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

      {/* Campos do formulário */}
      <div className={styles.formFields}>
        {/* Campo: Nome Completo */}
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

        {/* Linha com 2 colunas: CPF e Data de Nascimento */}
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

        {/* Linha com 2 colunas: Telefone e E-mail */}
        <div className={styles.row}>
          <FormField
            label={
              <>
                Telefone <span className={styles.required}>*</span>
              </>
            }
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
            label={
              <>
                E-mail <span className={styles.required}>*</span>
              </>
            }
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

      {/* Rodapé com botões de navegação */}
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
