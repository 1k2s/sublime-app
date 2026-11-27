package br.com.senai.sublime_app;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;

import jakarta.persistence.EntityManagerFactory;

// Conta as consultas ao banco de uma ação, nos testes de integração (o N+1: o número não pode
// crescer com o número de registros). Liga as estatísticas do Hibernate só durante a ação: ligá-las
// pelo perfil de teste criaria outro contexto do Spring e outro MySQL (ver @IntegrationTest)
public final class QueryCounter {

    private QueryCounter() {
    }

    public static long countQueries(EntityManagerFactory entityManagerFactory, Runnable action) {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        statistics.setStatisticsEnabled(true);
        try {
            action.run();
            return statistics.getPrepareStatementCount();
        } finally {
            statistics.setStatisticsEnabled(false);
        }
    }
}
