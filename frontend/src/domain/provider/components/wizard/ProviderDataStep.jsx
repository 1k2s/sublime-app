import { User, Percent, Info, ArrowRight } from "lucide-react";
import { FormField } from "@/shared/ui/FormField";
import { Input } from "@/shared/ui/Input";
import { Button } from "@/shared/ui/Button";
import styles from "./ProviderWizard.module.css";

export function ProviderDataStep({ register, errors, onCancel, onNext }) {
  return (
    <div className={styles.stepContainer}>
      <div className={styles.cardHeader}>
        <div className={styles.headerIconCircle}>
          <User size={22} />
        </div>
        <div>
          <h2 className={styles.cardTitle}>Dados do prestador</h2>
          <p className={styles.cardSubtitle}>
            Informe o nome do profissional e os percentuais de repasse.
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
          htmlFor="provider-name"
          error={errors.name?.message}
        >
          <Input
            id="provider-name"
            icon={User}
            placeholder="Dra. Beatriz Oliveira"
            invalid={Boolean(errors.name)}
            {...register("name")}
          />
        </FormField>

        <div className={styles.row}>
          <FormField
            label={
              <>
                Comissão padrão (%) <span className={styles.required}>*</span>
              </>
            }
            htmlFor="provider-commission"
            error={errors.commissionPercentage?.message}
          >
            <Input
              id="provider-commission"
              icon={Percent}
              placeholder="40"
              invalid={Boolean(errors.commissionPercentage)}
              {...register("commissionPercentage")}
            />
          </FormField>

          <FormField
            label={
              <>
                Comissão por indicação (%) <span className={styles.required}>*</span>
              </>
            }
            htmlFor="provider-referral-commission"
            error={errors.referralCommissionPercentage?.message}
          >
            <Input
              id="provider-referral-commission"
              icon={Percent}
              placeholder="45"
              invalid={Boolean(errors.referralCommissionPercentage)}
              {...register("referralCommissionPercentage")}
            />
          </FormField>
        </div>

        <div className={styles.helperBanner}>
          <Info size={18} className={styles.helperIcon} />
          <p className={styles.helperText}>
            <strong>Padrão da clínica:</strong> 40% para pacientes da clínica (ou
            trazidos por outros prestadores) e 45% para pacientes trazidos pelo
            próprio prestador. A comissão por indicação nunca pode ser menor que a
            comissão padrão.
          </p>
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
