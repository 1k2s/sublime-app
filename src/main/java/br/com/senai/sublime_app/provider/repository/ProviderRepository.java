package br.com.senai.sublime_app.provider.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.senai.sublime_app.provider.domain.ProviderEntity;

public interface ProviderRepository extends JpaRepository<ProviderEntity, Long> {
    
}
