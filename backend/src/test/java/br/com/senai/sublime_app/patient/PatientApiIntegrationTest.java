package br.com.senai.sublime_app.patient;

import static br.com.senai.sublime_app.ApiTestClient.body;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import br.com.senai.sublime_app.ApiScenario;
import br.com.senai.sublime_app.ApiTestClient.ApiResponse;
import br.com.senai.sublime_app.IntegrationTest;

// O cadastro do paciente pela API: o telefone, que só tem limite de tamanho (o schema guarda até
// 20 dígitos). Ver "Patient" no domain-model
@IntegrationTest
class PatientApiIntegrationTest {

    @Autowired
    private ApplicationContext context;

    private ApiScenario scenario;

    @BeforeEach
    void setUp() {
        scenario = new ApiScenario(context);
    }

    @Test
    @DisplayName("Telefone com até 20 dígitos: 201; com mais → 400 com a mensagem, sem chegar ao banco")
    void phoneLength() {
        ApiResponse created = scenario.api.post("/api/patients", patient("11987654321")).expect(201);

        scenario.api.post("/api/patients", patient("1".repeat(21)))
                .expectError(400, "O telefone deve ter no máximo 20 dígitos.");
        assertThat(created.<String>read("$.phone")).isEqualTo("11987654321");
    }

    private Map<String, Object> patient(String phone) {
        String cpf = String.format("%011d", System.nanoTime() % 100_000_000_000L);
        return body("name", "Paciente Telefone", "cpf", cpf, "birthDate", "1990-01-01", "phone", phone);
    }
}
