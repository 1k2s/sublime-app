package br.com.senai.sublime_app.contract.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import br.com.senai.sublime_app.pricing.dto.PlanResponseDTO;

// Apoio à tela de contrato (nada é gravado): vigência, opções do plano do item do Pilates em
// Grupo, valor de cada item, total e parcelas. As regras ficam no backend; o frontend não as
// repete. Ver "Valor e parcelas" em Contract, no domain-model
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
        // os meses do contrato (1 a 12)
        int maxInstallments,
        // as aplicadas: as pedidas, limitadas ao máximo (sem pedido, o máximo)
        int installments,
        // a primeira leva os centavos que sobram da divisão
        BigDecimal firstInstallmentValue,
        BigDecimal installmentValue

) {
}
