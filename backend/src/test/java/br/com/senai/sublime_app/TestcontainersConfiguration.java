package br.com.senai.sublime_app;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

// MySQL descartável para os testes de integração (ADR 0036). O @ServiceConnection aponta o
// datasource para o container, e o Flyway aplica as migrations nele ao subir o contexto.
// Nunca toca o banco de desenvolvimento.
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    // Versão exata, a mesma do docker-compose.yml (na raiz): mude nos dois. O
    // MySqlImageTest falha se os dois divergirem.
    static final String MYSQL_IMAGE = "mysql:8.0.46";

    @Bean
    @ServiceConnection
    MySQLContainer mysqlContainer() {
        return new MySQLContainer(DockerImageName.parse(MYSQL_IMAGE));
    }
}
