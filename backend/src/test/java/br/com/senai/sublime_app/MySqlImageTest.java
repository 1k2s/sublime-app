package br.com.senai.sublime_app;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// Os testes e o desenvolvimento rodam na mesma versão do MySQL (ADR 0036): a imagem do
// TestcontainersConfiguration tem de ser a do docker-compose.yml, na raiz do repositório
class MySqlImageTest {

    private static final Pattern IMAGE_LINE = Pattern.compile("^\\s*image:\\s*(\\S+)\\s*$", Pattern.MULTILINE);

    @Test
    @DisplayName("A imagem do MySQL dos testes é a mesma do docker-compose.yml")
    void testImageMatchesDockerCompose() throws IOException {
        String compose = Files.readString(findDockerCompose());

        Matcher image = IMAGE_LINE.matcher(compose);

        assertThat(image.find()).as("linha image: no docker-compose.yml").isTrue();
        assertThat(image.group(1)).isEqualTo(TestcontainersConfiguration.MYSQL_IMAGE);
    }

    // Sobe a partir da pasta de onde o teste roda (backend/ pelo Maven; a raiz por algumas IDEs)
    private static Path findDockerCompose() {
        for (Path dir = Path.of("").toAbsolutePath(); dir != null; dir = dir.getParent()) {
            Path compose = dir.resolve("docker-compose.yml");
            if (Files.exists(compose)) {
                return compose;
            }
        }
        throw new IllegalStateException("docker-compose.yml não encontrado acima de " + Path.of("").toAbsolutePath());
    }
}
