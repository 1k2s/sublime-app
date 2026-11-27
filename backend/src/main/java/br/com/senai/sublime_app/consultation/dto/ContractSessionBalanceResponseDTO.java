package br.com.senai.sublime_app.consultation.dto;

import br.com.senai.sublime_app.contract.domain.ContractItemEntity;

// Saldo de cada item do contrato, somando toda a cadeia de versões (aditivos). No item por
// duração, as sessões; no Pilates em Grupo, as aulas (as que cabem na vigência − as lançadas):
// só controle de presença, sem efeito na mensalidade. Usado na tela do aditivo e na conta do
// paciente.
public record ContractSessionBalanceResponseDTO(

        Long pricingGroupId,
        String pricingGroupName,
        Long techniqueId,
        String techniqueName,
        // o item do Pilates em Grupo (aulas, não sessões)
        boolean groupItem,
        int sessionCount,
        int usedSessions,
        // pode ficar negativo (saldo esgotado: o atendimento é aceito)
        int remainingSessions

) {
    // As sessões contratadas vêm do item (a linha travada e, no Grupo, a vigência), não do plano
    // do contrato: no contrato de mensalidade, a linha do item por duração é do pacote equivalente
    public static ContractSessionBalanceResponseDTO fromItem(ContractItemEntity item, int usedSessions) {
        int sessionCount = item.getValue().sessionCount();
        return new ContractSessionBalanceResponseDTO(
                item.getPricingGroup().getId(),
                item.getPricingGroup().getName(),
                item.getTechnique().getId(),
                item.getTechnique().getName(),
                !item.isDurationBased(),
                sessionCount,
                usedSessions,
                sessionCount - usedSessions);
    }
}
