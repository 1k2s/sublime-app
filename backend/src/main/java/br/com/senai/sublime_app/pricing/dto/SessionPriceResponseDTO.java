package br.com.senai.sublime_app.pricing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.com.senai.sublime_app.pricing.domain.SessionDurationPriceEntity;
import br.com.senai.sublime_app.pricing.domain.SessionFrequencyPriceEntity;

// Formato único para linhas das duas tabelas de preço. Mesma lógica do exclusive arc:
// só um dos eixos vem preenchido — durationMinutes (SessionDurationPrice) ou
// weeklyFrequency (SessionFrequencyPrice).
public record SessionPriceResponseDTO(

        Long id,
        Long planId,
        String planName,
        Integer sessionCount,
        Integer durationMinutes,
        Integer weeklyFrequency,
        BigDecimal sessionValue,
        LocalDate validFrom

) {
    public static SessionPriceResponseDTO fromEntity(SessionDurationPriceEntity entity) {
        return new SessionPriceResponseDTO(
                entity.getId(),
                entity.getPlan().getId(),
                entity.getPlan().getName(),
                entity.getPlan().getSessionCount(),
                entity.getDurationMinutes(),
                null,
                entity.getSessionValue().amount(),
                entity.getValidFrom());
    }

    public static SessionPriceResponseDTO fromEntity(SessionFrequencyPriceEntity entity) {
        return new SessionPriceResponseDTO(
                entity.getId(),
                entity.getPlan().getId(),
                entity.getPlan().getName(),
                entity.getPlan().getSessionCount(),
                null,
                entity.getWeeklyFrequency(),
                entity.getSessionValue().amount(),
                entity.getValidFrom());
    }
}
