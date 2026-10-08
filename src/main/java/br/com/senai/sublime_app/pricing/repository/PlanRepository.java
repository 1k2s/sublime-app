package br.com.senai.sublime_app.pricing.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.senai.sublime_app.pricing.domain.PlanEntity;

public interface PlanRepository extends JpaRepository<PlanEntity, Long> {

    // Checagem prévia de nome no cadastro. A garantia real é o unique da coluna;
    // este método só permite devolver uma mensagem clara em vez de um erro do banco.
    boolean existsByName(String name);
}
