package br.com.senai.sublime_app.pricing.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import br.com.senai.sublime_app.pricing.domain.PlanEntity;
import br.com.senai.sublime_app.pricing.enums.PlanType;

public interface PlanRepository extends JpaRepository<PlanEntity, Long> {

    // Checagem prévia de nome no cadastro. A garantia real é o unique da coluna;
    // este método só permite devolver uma mensagem clara em vez de um erro do banco.
    boolean existsByName(String name);

    // Só pode existir um plano avulso ativo: é o preço de quem é atendido sem contrato
    boolean existsByTypeAndActiveTrue(PlanType type);

    // Listagem com o equivalente do de-para junto (EntityGraph): a resposta mostra o
    // nome dele — sem isso, o Hibernate faria uma consulta extra por plano (N+1).
    @Override
    @EntityGraph(attributePaths = "equivalentPlan")
    List<PlanEntity> findAll();

    // Planos ativos de um tipo, do prazo mais curto ao mais longo. Usado pelo contract
    // para montar as opções do plano do Pilates em Grupo (mensalidades até a primeira
    // que cubra a vigência do contrato).
    List<PlanEntity> findByTypeAndActiveTrueOrderByTermWeeksAsc(PlanType type);
}
