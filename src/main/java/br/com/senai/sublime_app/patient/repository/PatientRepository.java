package br.com.senai.sublime_app.patient.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.senai.sublime_app.patient.domain.PatientEntity;

public interface PatientRepository extends JpaRepository<PatientEntity, Long> {

    // Checagem prévia de CPF no cadastro. A garantia real é o unique da coluna;
    // este método só permite devolver uma mensagem clara em vez de um erro do banco.
    boolean existsByCpf(String cpf);

    // Mesma checagem na atualização, ignorando o próprio paciente (senão editar
    // sem trocar o CPF seria rejeitado).
    boolean existsByCpfAndIdNot(String cpf, Long id);
}
