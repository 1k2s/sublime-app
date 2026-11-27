package br.com.senai.sublime_app;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.jayway.jsonpath.JsonPath;

import tools.jackson.databind.ObjectMapper;

// As requisições dos testes de integração pela API (MockMvc): o mesmo caminho de uma chamada real
// (a segurança com o token, a validação do DTO, o controller, o JSON e o código HTTP), sem subir um
// servidor. O token sai do POST /api/auth/login de verdade. Ver "Testes do backend" no architecture
public final class ApiTestClient {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private String token;

    public ApiTestClient(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    // Entra pelo login real e guarda o token para as próximas requisições
    public void login(String email, String password) {
        ApiResponse response = post("/api/auth/login", body("email", email, "password", password)).expect(200);
        token = response.read("$.token");
    }

    public ApiResponse get(String path) {
        return send(MockMvcRequestBuilders.get(path), null);
    }

    public ApiResponse post(String path, Object body) {
        return send(MockMvcRequestBuilders.post(path), body);
    }

    public ApiResponse delete(String path) {
        return send(MockMvcRequestBuilders.delete(path), null);
    }

    // O corpo JSON a partir de pares chave e valor, aceitando nulo (o Map.of não aceita)
    public static Map<String, Object> body(Object... keysAndValues) {
        Map<String, Object> body = new LinkedHashMap<>();
        for (int index = 0; index < keysAndValues.length; index += 2) {
            body.put((String) keysAndValues[index], keysAndValues[index + 1]);
        }
        return body;
    }

    private ApiResponse send(MockHttpServletRequestBuilder request, Object body) {
        try {
            if (token != null) {
                request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            }
            if (body != null) {
                request.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
            }
            MvcResult result = mockMvc.perform(request).andReturn();
            return new ApiResponse(result.getResponse().getStatus(),
                    result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("Falha na requisição do teste", exception);
        }
    }

    // A resposta: o código HTTP e o corpo, lido pelo JsonPath
    public record ApiResponse(int status, String body) {

        // Confere o código (com o corpo na mensagem, para a falha dizer o porquê)
        public ApiResponse expect(int expectedStatus) {
            assertThat(status).as(body).isEqualTo(expectedStatus);
            return this;
        }

        // Confere um erro: o código e a mensagem em português no corpo (o detail ou o errors)
        public ApiResponse expectError(int expectedStatus, String message) {
            assertThat(status).as(body).isEqualTo(expectedStatus);
            assertThat(body).contains(message);
            return this;
        }

        public <T> T read(String path) {
            return JsonPath.read(body, path);
        }

        // Um valor em dinheiro (a API devolve número JSON): BigDecimal, para comparar sem arredondar
        public BigDecimal decimal(String path) {
            Object value = read(path);
            return value != null ? new BigDecimal(value.toString()) : null;
        }

        public Long longAt(String path) {
            Number value = read(path);
            return value != null ? value.longValue() : null;
        }

        public Long id() {
            return longAt("$.id");
        }
    }
}
