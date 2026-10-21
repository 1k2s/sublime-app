package br.com.senai.sublime_app.consultation.dto;

import br.com.senai.sublime_app.contract.domain.ContractItemEntity;

// Saldo de sessões de um item por duração do contrato, somando toda a cadeia de
// versões (aditivos). Usado na tela do aditivo para o admin decidir (prorrogar, mais
// sessões). O item do Pilates em Grupo não tem saldo e não aparece.
public record ContractSessionBalanceResponseDTO(

        Long pricingGroupId,
        String pricingGroupName,
        Long techniqueId,
        String techniqueName,
        int sessionCount,
        int usedSessions,
        // pode ficar negativo (saldo esgotado: o atendimento é aceito)
        int remainingSessions

) {
    public static ContractSessionBalanceResponseDTO fromItem(ContractItemEntity item, int sessionCount, int usedSessions) {
        return new ContractSessionBalanceResponseDTO(
                item.getPricingGroup().getId(),
                item.getPricingGroup().getName(),
                item.getTechnique().getId(),
                item.getTechnique().getName(),
                sessionCount,
                usedSessions,
                sessionCount - usedSessions);
    }
}
