package br.com.senai.sublime_app.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.senai.sublime_app.user.domain.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    // Checagem prévia de e-mail no cadastro. A garantia real é o unique da coluna;
    // este método só permite devolver uma mensagem clara em vez de um erro do banco.
    boolean existsByEmail(String email);

    // Mesma checagem na atualização, ignorando o próprio usuário (senão editar
    // sem trocar o e-mail seria rejeitado).
    boolean existsByEmailAndIdNot(String email, Long id);
}
