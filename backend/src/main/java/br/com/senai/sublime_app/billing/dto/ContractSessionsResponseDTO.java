package br.com.senai.sublime_app.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import br.com.senai.sublime_app.billing.domain.ContractSessions;
import br.com.senai.sublime_app.consultation.domain.ConsultationEntity;
import br.com.senai.sublime_app.consultation.domain.ConsultationStatus;

// Os atendimentos ativos do contrato (a aba Sessões), numerados ("7ª de 6"). hasBeneficiary:
// a tela mostra a coluna "Paciente"
public record ContractSessionsResponseDTO(boolean hasBeneficiary, List<SessionDTO> sessions) {

    // number e contractedCount nulos no de-para; contractedCount nulo no item removido; tag nula
    // dentro do contratado
    public record SessionDTO(
            Long consultationId,
            LocalDate occurredAt,
            String techniqueName,
            int durationMinutes,
            String patientName,
            String providerName,
            ConsultationStatus status,
            BigDecimal baseValue,
            Integer number,
            Integer contractedCount,
            ContractSessions.Tag tag) {

        static SessionDTO fromSession(ContractSessions.Session session) {
            ConsultationEntity consultation = session.consultation();
            return new SessionDTO(
                    consultation.getId(),
                    consultation.getOccurredAt(),
                    consultation.getTechnique().getName(),
                    consultation.getDurationMinutes(),
                    consultation.getPatient().getName(),
                    consultation.getProvider().getName(),
                    consultation.getStatus(),
                    consultation.getBaseValue().amount(),
                    session.number(),
                    session.contractedCount(),
                    session.tag());
        }
    }

    public static ContractSessionsResponseDTO fromSessions(ContractSessions sessions) {
        return new ContractSessionsResponseDTO(sessions.hasBeneficiary(),
                sessions.sessions().stream().map(SessionDTO::fromSession).toList());
    }
}
