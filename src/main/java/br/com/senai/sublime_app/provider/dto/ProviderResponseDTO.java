package br.com.senai.sublime_app.provider.dto;

import java.math.BigDecimal;

import br.com.senai.sublime_app.provider.domain.ProviderEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProviderResponseDTO {

    private Long id;
    private Long userId;
    private String name;
    private BigDecimal commissionPercentage;
    private boolean active;

    public ProviderResponseDTO(ProviderEntity provider) {
        this.id = provider.getId();
        this.userId = provider.getUser() != null ? provider.getUser().getId() : null;
        this.name = provider.getName();
        this.commissionPercentage = provider.getCommissionPercentage();
        this.active = provider.isActive();
    }
}
