package br.com.senai.sublime_app.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.senai.sublime_app.user.domain.User;

public interface UserRepository extends JpaRepository<User, Long> {
}
