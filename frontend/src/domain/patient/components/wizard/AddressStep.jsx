// Etapa 2 do cadastro de paciente: endereço opcional (completo quando informado: cep, street, numberHouse, city)
import { useState } from "react";
import { useWatch } from "react-hook-form";
import { ArrowLeft, ArrowRight, Loader2, MapPin } from "lucide-react";
import { hasAddressInput } from "@/domain/patient/schemas/patientSchema";
import { Button } from "@/shared/ui/Button";
import { FormField } from "@/shared/ui/FormField";
import { Input } from "@/shared/ui/Input";
import { maskCep, onlyDigits } from "@/shared/utils/masks";
import { fetchAddressByCep } from "@/shared/utils/viacep";
import styles from "@/domain/patient/components/wizard/PatientWizard.module.css";

export function AddressStep({
  register,
  control,
  errors,
  setValue,
  onBack,
  onNext,
}) {
  const [loadingCep, setLoadingCep] = useState(false);
  const values = useWatch({ control });
  const cepValue = values.cep || "";
  const isRequired = hasAddressInput(values);

  async function handleCepChange(e) {
    const masked = maskCep(e.target.value);
    setValue("cep", masked, { shouldValidate: true });

    const digitsOnly = onlyDigits(masked);
    if (digitsOnly.length === 8) {
      setLoadingCep(true);
      const address = await fetchAddressByCep(digitsOnly);
      setLoadingCep(false);

      if (address) {
        if (address.street) setValue("street", address.street, { shouldValidate: true });
        if (address.city) setValue("city", address.city, { shouldValidate: true });
        if (address.complement) setValue("complement", address.complement);
      }
    }
  }

  return (
    <div className={styles.stepContainer}>
      <div className={styles.cardHeader}>
        <div className={styles.headerIconCircle}>
          <MapPin size={22} />
        </div>
        <div>
          <h2 className={styles.cardTitle}>Endereço</h2>
          <p className={styles.cardSubtitle}>
            {values.addressRequired
              ? "Atualize o endereço residencial do paciente."
              : "Opcional no cadastro. Quando informado, preencha CEP, rua, número e cidade."}
          </p>
        </div>
      </div>

      <div className={styles.formFields}>
        <div className={styles.row}>
          <div className={styles.cepWrapper}>
            <FormField
              label={
                <>
                  CEP {isRequired && <span className={styles.required}>*</span>}
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
                value={cepValue}
                onChange={handleCepChange}
                invalid={Boolean(errors.cep)}
              />
            </FormField>
          </div>

          <FormField
            label={
              <>
                Rua / Logradouro {isRequired && <span className={styles.required}>*</span>}
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

        <div className={styles.row}>
          <FormField
            label={
              <>
                Número {isRequired && <span className={styles.required}>*</span>}
              </>
            }
            htmlFor="numberHouse"
            error={errors.numberHouse?.message}
          >
            <Input
              id="numberHouse"
              placeholder="123 ou S/N"
              invalid={Boolean(errors.numberHouse)}
              {...register("numberHouse")}
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

        <FormField
          label={
            <>
              Cidade {isRequired && <span className={styles.required}>*</span>}
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
