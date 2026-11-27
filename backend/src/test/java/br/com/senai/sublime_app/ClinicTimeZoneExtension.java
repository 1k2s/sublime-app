package br.com.senai.sublime_app;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

// O fuso da clínica antes de o Spring subir, nos testes de integração (ADR 0034). Em produção, o
// bloco static do SublimeAppApplication roda antes do main, e portanto antes de qualquer conexão
// com o banco. Nos testes não há main: o Spring pode abrir as conexões do pool (para o Flyway) com
// a JVM ainda em UTC (o argLine do surefire) e só depois carregar a classe que troca o fuso. A
// conexão aberta em UTC converte as datas lidas no fuso errado (o dia anterior). Esta extensão
// carrega a classe antes (o mesmo bloco, o fuso num lugar só); a @IntegrationTest a declara antes
// do @SpringBootTest, para rodar primeiro
public class ClinicTimeZoneExtension implements BeforeAllCallback {

    @Override
    public void beforeAll(ExtensionContext context) throws ClassNotFoundException {
        // Class.forName inicializa a classe (roda o bloco static); a referência .class não
        Class.forName(SublimeAppApplication.class.getName());
    }
}
