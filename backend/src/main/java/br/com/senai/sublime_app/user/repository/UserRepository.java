package br.com.senai.sublime_app.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.senai.sublime_app.user.domain.UserEntity;
import br.com.senai.sublime_app.user.enums.Role;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    // Checagem prévia de e-mail no cadastro. A garantia real é o unique da coluna;
    // este método só permite devolver uma mensagem clara em vez de um erro do banco.
    boolean existsByEmail(String email);

    // Mesma checagem na atualização, ignorando o próprio usuário (senão editar
    // sem trocar o e-mail seria rejeitado).
    boolean existsByEmailAndIdNot(String email, Long id);

    // Login: busca o usuário pelo e-mail (a collation do MySQL ignora maiúsculas)
    Optional<UserEntity> findByEmail(String email);

    // Existe algum usuário ativo com este perfil? Usado ao subir a aplicação, para criar
    // o primeiro ADMIN só quando não houver nenhum
    boolean existsByRoleAndActiveTrue(Role role);
}
