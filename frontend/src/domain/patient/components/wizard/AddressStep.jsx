/**
 * ======================================================================================
 * COMPONENTE: AddressStep (Etapa 2 do Wizard)
 * ======================================================================================
 *
 * Apresenta os campos de endereço residencial do paciente:
 * - CEP (com máscara `00000-000` e busca automática pelo ViaCEP ao completar os 8 dígitos).
 * - Logradouro, Número, Complemento, Bairro, Cidade e UF.
 * - Feedback visual de carregamento (`Loader2`) enquanto o ViaCEP consulta os dados.
 * - Botões: "← Voltar" (retorna à Etapa 1) e "Próximo →" (valida e avança para a revisão).
 */

import { useState } from "react";
import { MapPin, ArrowLeft, ArrowRight, Loader2 } from "lucide-react";
import { FormField } from "@/shared/ui/FormField";
import { Input } from "@/shared/ui/Input";
import { Button } from "@/shared/ui/Button";
import { maskCep } from "@/shared/utils/masks";
import { fetchAddressByCep } from "@/shared/utils/viacep";
import styles from "./PatientWizard.module.css";

/**
 * @param {Object} props
 * @param {import("react-hook-form").UseFormRegister} props.register - Função register do React Hook Form
 * @param {Object} props.errors - Objeto com os erros de validação dos campos de endereço
 * @param {import("react-hook-form").UseFormSetValue} props.setValue - Função para alterar valores programaticamente
 * @param {import("react-hook-form").UseFormWatch} props.watch - Função para observar o valor dos campos
 * @param {function(): void} props.onBack - Retorna para a etapa 1 (Dados Pessoais)
 * @param {function(): void} props.onNext - Valida e avança para a etapa 3 (Confirmação)
 */
export function AddressStep({
  register,
  errors,
  setValue,
  watch,
  onBack,
  onNext,
}) {
  // Estado local para indicar busca de endereço em andamento pelo ViaCEP
  const [loadingCep, setLoadingCep] = useState(false);
  const cepValue = watch("cep");

  /**
   * Monitora a digitação do CEP:
   * 1. Aplica a máscara visual (`00000-000`).
   * 2. Se atingir 8 dígitos numéricos, dispara a requisição assíncrona ao ViaCEP.
   * 3. Preenche automaticamente logradouro, bairro, cidade, UF e complemento.
   */
  const handleCepChange = async (e) => {
    const masked = maskCep(e.target.value);
    setValue("cep", masked, { shouldValidate: true });

    const digitsOnly = masked.replace(/\D/g, "");
    if (digitsOnly.length === 8) {
      setLoadingCep(true);
      const address = await fetchAddressByCep(digitsOnly);
      setLoadingCep(false);

      if (address) {
        if (address.street) setValue("street", address.street, { shouldValidate: true });
        if (address.neighborhood) setValue("neighborhood", address.neighborhood, { shouldValidate: true });
        if (address.city) setValue("city", address.city, { shouldValidate: true });
        if (address.state) setValue("state", address.state, { shouldValidate: true });
        if (address.complement) setValue("complement", address.complement);
      }
    }
  };

  return (
    <div className={styles.stepContainer}>
      {/* Cabeçalho da Etapa 2 */}
      <div className={styles.cardHeader}>
        <div className={styles.headerIconCircle}>
          <MapPin size={22} />
        </div>
        <div>
          <h2 className={styles.cardTitle}>Endereço</h2>
          <p className={styles.cardSubtitle}>
            Informe o endereço de residência do paciente.
          </p>
        </div>
      </div>

      {/* Formulário de Endereço */}
      <div className={styles.formFields}>
        {/* Linha com CEP e Logradouro */}
        <div className={styles.row}>
          <div className={styles.cepWrapper}>
            <FormField
              label={
                <>
                  CEP <span className={styles.required}>*</span>
                  {/* Feedback visual de busca automática */}
                  {loadingCep && (
                    <span className={styles.cepLoading}>
                      <Loader2 size={12} className={styles.spin} /> Buscando endereço...
                    </span>
                  )}
                </>
              }
              htmlFor="cep"
              error={errors.cep?.message}
            >
              <Input
                id="cep"
                placeholder="13560-000"
                value={cepValue || ""}
                onChange={handleCepChange}
                invalid={Boolean(errors.cep)}
              />
            </FormField>
          </div>

          <FormField
            label={
              <>
                Logradouro <span className={styles.required}>*</span>
              </>
            }
            htmlFor="street"
            error={errors.street?.message}
          >
            <Input
              id="street"
              placeholder="Rua Episcopal"
              invalid={Boolean(errors.street)}
              {...register("street")}
            />
          </FormField>
        </div>

        {/* Linha com Número e Complemento */}
        <div className={styles.row}>
          <FormField
            label={
              <>
                Número <span className={styles.required}>*</span>
              </>
            }
            htmlFor="number"
            error={errors.number?.message}
          >
            <Input
              id="number"
              placeholder="123"
              invalid={Boolean(errors.number)}
              {...register("number")}
            />
          </FormField>

          <FormField
            label="Complemento"
            htmlFor="complement"
            error={errors.complement?.message}
          >
            <Input
              id="complement"
              placeholder="Apto 42, Bloco B"
              invalid={Boolean(errors.complement)}
              {...register("complement")}
            />
          </FormField>
        </div>

        {/* Linha com 3 colunas: Bairro, Cidade e UF */}
        <div className={styles.rowThree}>
          <FormField
            label={
              <>
                Bairro <span className={styles.required}>*</span>
              </>
            }
            htmlFor="neighborhood"
            error={errors.neighborhood?.message}
          >
            <Input
              id="neighborhood"
              placeholder="Centro"
              invalid={Boolean(errors.neighborhood)}
              {...register("neighborhood")}
            />
          </FormField>

          <FormField
            label={
              <>
                Cidade <span className={styles.required}>*</span>
              </>
            }
            htmlFor="city"
            error={errors.city?.message}
          >
            <Input
              id="city"
              placeholder="São Carlos"
              invalid={Boolean(errors.city)}
              {...register("city")}
            />
          </FormField>

          <FormField
            label={
              <>
                UF <span className={styles.required}>*</span>
              </>
            }
            htmlFor="state"
            error={errors.state?.message}
          >
            <Input
              id="state"
              placeholder="SP"
              maxLength={2}
              invalid={Boolean(errors.state)}
              {...register("state")}
            />
          </FormField>
        </div>
      </div>

      {/* Rodapé com botões de voltar e avançar */}
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
