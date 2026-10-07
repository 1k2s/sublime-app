package br.com.senai.sublime_app.pricing.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import br.com.senai.sublime_app.pricing.domain.TechniqueEntity;

public interface TechniqueRepository extends JpaRepository<TechniqueEntity, Long> {

    // Técnicas disponíveis para novos contratos/atendimentos. O grupo vem junto
    // (EntityGraph) porque a tela precisa do pricingModel de cada técnica —
    // sem isso, o Hibernate faria uma consulta extra por técnica (N+1).
    @EntityGraph(attributePaths = "pricingGroup")
    List<TechniqueEntity> findByActiveTrue();

    // Checagem prévia de nome no cadastro. A garantia real é o unique da coluna;
    // este método só permite devolver uma mensagem clara em vez de um erro do banco.
    boolean existsByName(String name);
}
