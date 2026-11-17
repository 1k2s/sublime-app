package br.com.senai.sublime_app;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SublimeAppApplication {

	// Fuso da clínica (fica num lugar só)
	static final String CLINIC_TIME_ZONE = "America/Sao_Paulo";

	// "Hoje" é o dia da clínica, qualquer que seja o fuso do servidor (num servidor em UTC,
	// depois das 21h o LocalDate.now() já seria o dia seguinte). Bloco static, e não no main:
	// roda quando a classe é carregada, antes do Spring subir, tanto em produção (antes do
	// main) quanto nos testes de integração, que carregam a classe sem chamar o main. Vale
	// para todo now() e para o @PastOrPresent. Ver o ADR 0034.
	static {
		TimeZone.setDefault(TimeZone.getTimeZone(CLINIC_TIME_ZONE));
	}

	public static void main(String[] args) {
		SpringApplication.run(SublimeAppApplication.class, args);
	}

}
