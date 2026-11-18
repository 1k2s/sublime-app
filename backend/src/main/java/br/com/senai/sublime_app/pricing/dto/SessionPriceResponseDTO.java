package br.com.senai.sublime_app.pricing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.com.senai.sublime_app.pricing.domain.SessionDurationPriceEntity;
import br.com.senai.sublime_app.pricing.domain.SessionFrequencyPriceEntity;
import br.com.senai.sublime_app.pricing.enums.PriceStatus;

// Formato único para linhas das duas tabelas de preço. Mesma lógica do exclusive arc:
// só um dos eixos vem preenchido — durationMinutes (SessionDurationPrice) ou
// weeklyFrequency (SessionFrequencyPrice). validTo nulo = aberta (CURRENT ou EXPIRED se passou de
// 12 meses sem reajuste); preenchido = encerrada por um reajuste (CLOSED; exclusivo: a linha valeu
// até o dia anterior; ver a convenção de datas no domain-model).
public record SessionPriceResponseDTO(

        Long id,
        Long planId,
        String planName,
        Integer sessionCount,
        Integer durationMinutes,
        Integer weeklyFrequency,
        BigDecimal sessionValue,
        LocalDate validFrom,
        LocalDate validTo,
        PriceStatus status

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
                entity.getValidFrom(),
                entity.getValidTo(),
                entity.statusOn(LocalDate.now()));
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
                entity.getValidFrom(),
                entity.getValidTo(),
                entity.statusOn(LocalDate.now()));
    }
}
