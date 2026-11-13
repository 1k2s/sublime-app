package br.com.senai.sublime_app;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SublimeAppApplication {

	// Fuso da clínica (fica num lugar só)
	private static final String CLINIC_TIME_ZONE = "America/Sao_Paulo";

	public static void main(String[] args) {
		// "Hoje" é o dia da clínica, qualquer que seja o fuso do servidor (num servidor em
		// UTC, depois das 21h o LocalDate.now() já seria o dia seguinte). Fixado antes do
		// Spring subir: vale para todo now() e para o @PastOrPresent
		TimeZone.setDefault(TimeZone.getTimeZone(CLINIC_TIME_ZONE));
		SpringApplication.run(SublimeAppApplication.class, args);
	}

}
