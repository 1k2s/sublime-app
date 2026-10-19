package br.com.senai.sublime_app.contract.dto;

import java.time.LocalDate;
import java.util.List;

import br.com.senai.sublime_app.pricing.dto.PlanResponseDTO;

// Apoio à tela de contrato (nada é gravado): vigência calculada e opções do plano do
// item do Pilates em Grupo. A regra fica no backend; o frontend não a repete.
public record ContractTermPreviewResponseDTO(

        int durationWeeks,
        // fim inclusivo
        LocalDate endDate,
        // de-para a partir do plano do contrato; null se não houver equivalente nas opções
        Long suggestedGroupPlanId,
        // mensalidades permitidas para o item do Grupo (até a primeira que cubra a vigência)
        List<PlanResponseDTO> groupPlanOptions

) {
}
