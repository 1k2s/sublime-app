package br.com.senai.sublime_app.provider.dto;

import java.time.LocalDate;

import br.com.senai.sublime_app.provider.domain.RetroactiveLaunchGrantEntity;

public record RetroactiveGrantResponseDTO(

        Long id,
        Long providerId,
        // o dia em que vale (até 23:59)
        LocalDate grantedOn,
        int allowedCount,
        int usedCount,
        int remaining,
        Long grantedByUserId

) {
    public static RetroactiveGrantResponseDTO fromEntity(RetroactiveLaunchGrantEntity entity) {
        return new RetroactiveGrantResponseDTO(
                entity.getId(),
                entity.getProvider().getId(),
                entity.getGrantedOn(),
                entity.getAllowedCount(),
                entity.getUsedCount(),
                entity.getRemaining(),
                entity.getGrantedBy().getId());
    }
}
