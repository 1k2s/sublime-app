package br.com.senai.sublime_app.provider.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.senai.sublime_app.provider.domain.ProviderEntity;

public interface ProviderRepository extends JpaRepository<ProviderEntity, Long> {
    
    // Verifica se já existe um prestador vinculado ao ID de usuário informado (garante unicidade)
    boolean existsByUserId(Long userId);

    // Prestador do usuário logado (vínculo 1:1). Vazio = usuário sem prestador
    Optional<ProviderEntity> findByUserId(Long userId);
}
