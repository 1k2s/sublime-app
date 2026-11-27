package br.com.senai.sublime_app.contract.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import br.com.senai.sublime_app.pricing.dto.PlanResponseDTO;

// Apoio à tela de contrato (nada é gravado): vigência, opções do plano do item do Pilates em
// Grupo, valor de cada item, total, parcelas e o cronograma. As regras ficam no backend; o
// frontend não as repete. Ver "Valor e parcelas" em Contract, no domain-model
public record ContractPreviewResponseDTO(

        int durationWeeks,
        // fim inclusivo
        LocalDate endDate,
        // de-para a partir do plano do contrato; null se não houver equivalente nas opções
        Long suggestedGroupPlanId,
        // mensalidades permitidas para o item do Grupo (até a primeira que cubra a vigência)
        List<PlanResponseDTO> groupPlanOptions,
        // na ordem dos itens enviados
        List<ContractItemPreviewDTO> items,
        BigDecimal totalValue,
        // no aditivo, o pago até agora (a cadeia de versões); null no cadastro
        BigDecimal paidSoFar,
        // o valor parcelado: o total, no cadastro; o total − o pago, no aditivo (mínimo 0)
        BigDecimal installmentBase,
        // o máximo de parcelas (1 a 12): no cadastro, os meses do contrato; no aditivo, os meses
        // do 1º vencimento ao novo fim
        int maxInstallments,
        // as aplicadas: as pedidas, limitadas ao máximo (sem pedido, o máximo)
        int installments,
        // a primeira leva os centavos que sobram da divisão
        BigDecimal firstInstallmentValue,
        BigDecimal installmentValue,
        // o 1º vencimento aplicado (o pedido ou, sem ele, o padrão) e o sugerido (só no aditivo)
        LocalDate firstDueDate,
        LocalDate suggestedFirstDueDate,
        // vencimento e valor de cada parcela
        List<InstallmentResponseDTO> installmentSchedule,
        // no cadastro, o crédito disponível do titular e o aplicado (o que cabe no total); null no
        // aditivo
        BigDecimal availableCredit,
        BigDecimal creditApplied

) {
}
