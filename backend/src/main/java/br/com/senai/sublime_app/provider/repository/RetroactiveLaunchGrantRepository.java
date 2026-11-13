package br.com.senai.sublime_app.provider.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import br.com.senai.sublime_app.provider.domain.RetroactiveLaunchGrantEntity;
import jakarta.persistence.LockModeType;

public interface RetroactiveLaunchGrantRepository extends JpaRepository<RetroactiveLaunchGrantEntity, Long> {

    // A liberação que vale no dia: a mais recente (liberar de novo cria outra)
    Optional<RetroactiveLaunchGrantEntity> findFirstByProviderIdAndGrantedOnOrderByIdDesc(Long providerId,
            LocalDate grantedOn);

    // A mesma busca, trancando a linha até o fim do lançamento (SELECT ... FOR UPDATE): dois
    // lançamentos retroativos ao mesmo tempo não usam a mesma unidade; o segundo espera e
    // vê o saldo já consumido
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RetroactiveLaunchGrantEntity> findFirstWithLockByProviderIdAndGrantedOnOrderByIdDesc(Long providerId,
            LocalDate grantedOn);
}
