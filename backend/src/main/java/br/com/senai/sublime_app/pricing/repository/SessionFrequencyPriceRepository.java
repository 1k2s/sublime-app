package br.com.senai.sublime_app.pricing.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import br.com.senai.sublime_app.pricing.domain.SessionFrequencyPriceEntity;

public interface SessionFrequencyPriceRepository extends JpaRepository<SessionFrequencyPriceEntity, Long> {

    // Linhas vigentes (validTo = null) de um grupo. O plano vem junto (EntityGraph)
    // porque cada linha é exibida com o nome do plano — evita uma consulta por linha (N+1).
    @EntityGraph(attributePaths = "plan")
    List<SessionFrequencyPriceEntity> findByPricingGroupIdAndValidToIsNull(Long pricingGroupId);

    // Todas as linhas de um grupo, vigentes e encerradas (histórico da tela de preços), da mais
    // recente para a mais antiga. O plano vem junto (EntityGraph), como na busca das vigentes.
    @EntityGraph(attributePaths = "plan")
    List<SessionFrequencyPriceEntity> findByPricingGroupIdOrderByValidFromDescIdDesc(Long pricingGroupId);

    // Linha vigente de uma combinação (grupo, plano, frequência). Pela regra de unicidade
    // existe no máximo uma: vazio = preço novo; presente = reajuste (fechar e criar outra).
    Optional<SessionFrequencyPriceEntity> findByPricingGroupIdAndPlanIdAndWeeklyFrequencyAndValidToIsNull(
            Long pricingGroupId, Long planId, int weeklyFrequency);
}
