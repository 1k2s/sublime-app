package br.com.senai.sublime_app.billing.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import br.com.senai.sublime_app.consultation.domain.ConsultationEntity;
import br.com.senai.sublime_app.consultation.domain.PricingSource;
import br.com.senai.sublime_app.contract.domain.ContractEntity;
import br.com.senai.sublime_app.contract.domain.ContractItemEntity;
import br.com.senai.sublime_app.pricing.domain.PricingGroupEntity;

// Os atendimentos de um contrato numerados ("7ª de 6"), a aba Sessões da conta. Ver "Numeração
// das sessões" em Payment, no domain-model:
// - só os ativos (o inativado não é cobrado); inativar um antigo renumera os seguintes;
// - numerados por item (o grupo de preço), na cadeia de versões toda, pela data e, no mesmo dia,
//   pela ordem do lançamento (o id); o contratado é o do item na versão atual;
// - acima do contratado: "além do contratado" (cobrada) ou, no Grupo, "além das aulas previstas"
//   (sem valor a mais);
// - o de-para: sem número, "fora do contrato"; o item que o aditivo removeu: as sessões
//   continuam numeradas, sem o contratado, com "item removido no aditivo".
public final class ContractSessions {

    public enum Tag {
        BEYOND_CONTRACT,
        BEYOND_CLASSES,
        OUT_OF_CONTRACT,
        REMOVED_ITEM
    }

    // number e contractedCount nulos no de-para; contractedCount nulo no item removido; tag nula
    // na sessão dentro do contratado
    public record Session(ConsultationEntity consultation, Integer number, Integer contractedCount, Tag tag) {
    }

    private static final Comparator<ConsultationEntity> LAUNCH_ORDER = Comparator
            .comparing(ConsultationEntity::getOccurredAt)
            .thenComparing(ConsultationEntity::getId);

    private final boolean hasBeneficiary;
    private final List<Session> sessions;

    private ContractSessions(boolean hasBeneficiary, List<Session> sessions) {
        this.hasBeneficiary = hasBeneficiary;
        this.sessions = sessions;
    }

    // current = a versão atual; chainConsultations = os atendimentos da cadeia de versões
    public static ContractSessions of(ContractEntity current, List<ConsultationEntity> chainConsultations) {
        Map<Long, Integer> countByGroup = new HashMap<>();
        List<Session> sessions = new ArrayList<>();
        chainConsultations.stream()
                .filter(ConsultationEntity::isActive)
                .sorted(LAUNCH_ORDER)
                .forEach(consultation -> sessions.add(sessionOf(current, consultation, countByGroup)));
        return new ContractSessions(current.getBeneficiary() != null, List.copyOf(sessions));
    }

    private static Session sessionOf(ContractEntity current, ConsultationEntity consultation,
            Map<Long, Integer> countByGroup) {
        if (consultation.getPricingSource() != PricingSource.CONTRACT_ITEM) {
            return new Session(consultation, null, null, Tag.OUT_OF_CONTRACT);
        }
        PricingGroupEntity group = pricingGroupOf(consultation);
        int number = countByGroup.merge(group.getId(), 1, Integer::sum);
        Optional<ContractItemEntity> item = current.findItemOf(group);
        if (item.isEmpty()) {
            return new Session(consultation, number, null, Tag.REMOVED_ITEM);
        }
        int contracted = item.get().getValue().sessionCount();
        return new Session(consultation, number, contracted, tagOf(item.get(), number, contracted));
    }

    private static Tag tagOf(ContractItemEntity item, int number, int contracted) {
        if (number <= contracted) {
            return null;
        }
        return item.isDurationBased() ? Tag.BEYOND_CONTRACT : Tag.BEYOND_CLASSES;
    }

    // O grupo da linha de preço usada (o item do contrato é por grupo)
    private static PricingGroupEntity pricingGroupOf(ConsultationEntity consultation) {
        return consultation.getSessionDurationPrice() != null
                ? consultation.getSessionDurationPrice().getPricingGroup()
                : consultation.getSessionFrequencyPrice().getPricingGroup();
    }

    public boolean hasBeneficiary() {
        return hasBeneficiary;
    }

    public List<Session> sessions() {
        return sessions;
    }
}
