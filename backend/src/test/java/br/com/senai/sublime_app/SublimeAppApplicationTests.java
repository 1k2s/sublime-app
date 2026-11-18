package br.com.senai.sublime_app;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.TimeZone;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// A aplicação inteira no MySQL do Testcontainers (Flyway, JPA com ddl-auto=validate,
// segurança, admin padrão)
@IntegrationTest
class SublimeAppApplicationTests {

	@Test
	@DisplayName("A aplicação sobe: as entidades batem com as migrations")
	void contextLoads() {
	}

	// O Maven roda os testes numa JVM em UTC (pom.xml): o fuso da clínica só aparece aqui se
	// a própria aplicação o aplicar, mesmo sem o main (ver o ADR 0034)
	@Test
	@DisplayName("O \"hoje\" é o do fuso da clínica, também nos testes")
	void usesClinicTimeZone() {
		assertThat(TimeZone.getDefault().getID()).isEqualTo(SublimeAppApplication.CLINIC_TIME_ZONE);
	}

}
