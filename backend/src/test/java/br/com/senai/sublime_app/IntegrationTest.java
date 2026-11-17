package br.com.senai.sublime_app;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

// Teste de integração (ADR 0035): a aplicação inteira, com o MySQL do Testcontainers, o
// perfil "test" (application-test.properties) e o MockMvc para chamar a API.
// - Sem @Transactional de propósito: cada requisição tem a própria transação, como em
//   produção (um service sem @Transactional ou um lazy loading fora da transação falham
//   aqui também). O que o teste grava fica no banco até o fim da execução, então cada
//   teste cria dados únicos e nunca supõe uma tabela vazia.
// - Todas as classes com esta anotação dividem o mesmo contexto do Spring e, por isso, o
//   mesmo container: ele sobe uma vez por execução.
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public @interface IntegrationTest {
}
