package br.com.senai.sublime_app;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

// Teste de integração (ADR 0036): a aplicação inteira, com o MySQL do Testcontainers, o
// perfil "test" (application-test.properties) e o MockMvc para chamar a API.
// - Sem @Transactional de propósito: cada requisição tem a própria transação, como em
//   produção (uma alteração que o service não grava não aparece na requisição seguinte, e
//   o dado conferido vem do banco, não do cache do Hibernate). O lazy loading fora do
//   service não falha aqui nem em produção: o open-in-view do Spring vem ligado. O que o
//   teste grava fica no banco até o fim da execução, então cada teste cria dados únicos
//   e nunca supõe uma tabela vazia.
// - Todas as classes com esta anotação dividem o mesmo contexto do Spring e, por isso, o
//   mesmo container: ele sobe uma vez por execução.
// - O fuso da clínica é aplicado antes de o Spring subir (ClinicTimeZoneExtension, declarada
//   antes do @SpringBootTest): sem isso, as datas lidas do banco voltavam um dia antes.
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(ClinicTimeZoneExtension.class)
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public @interface IntegrationTest {
}
