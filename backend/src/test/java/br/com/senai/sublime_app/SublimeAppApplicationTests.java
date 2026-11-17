package br.com.senai.sublime_app;

import org.junit.jupiter.api.Test;

// Sobe a aplicação inteira (Flyway, JPA com ddl-auto=validate, segurança, admin padrão)
// no MySQL do Testcontainers: falha se as entidades não baterem com as migrations
@IntegrationTest
class SublimeAppApplicationTests {

	@Test
	void contextLoads() {
	}

}
