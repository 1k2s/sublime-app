package br.com.senai.sublime_app.contract.enums;

// Ritmo negociado num item de grupo por duração ("X sessões a cada Y semanas").
// Cardápio fechado: combinação que a clínica não vende não tem como existir.
// O cálculo do prazo mora aqui (princípio 6) — ver domain-model.md, "Vigência e frequência".
public enum SessionFrequency {

    // 1x por mês = 1 sessão a cada 4 semanas
    MONTHLY(1, 4),

    // Quinzenal = 1 sessão a cada 2 semanas
    BIWEEKLY(1, 2),

    ONCE_A_WEEK(1, 1),
    TWICE_A_WEEK(2, 1),
    THREE_TIMES_A_WEEK(3, 1),
    FOUR_TIMES_A_WEEK(4, 1),
    FIVE_TIMES_A_WEEK(5, 1);

    // Teto da vigência (1 ano): pacote longo em ritmo lento não pode ser esticado
    // (ex: Transformação 1x por mês = 104 semanas — não pode ser vendido)
    public static final int MAX_TERM_WEEKS = 52;

    private final int sessionsPerPeriod;
    private final int weeksPerPeriod;

    SessionFrequency(int sessionsPerPeriod, int weeksPerPeriod) {
        this.sessionsPerPeriod = sessionsPerPeriod;
        this.weeksPerPeriod = weeksPerPeriod;
    }

    // Quantas semanas leva para completar as sessões do plano nesse ritmo.
    // Semana quebrada arredonda para cima (26 sessões a 3x/semana = 8,67 → 9): o
    // paciente nunca fica com menos tempo que o necessário. Divisão inteira com
    // arredondamento para cima: (total + divisor - 1) / divisor.
    public int weeksToComplete(int sessionCount) {
        int totalWeeks = sessionCount * weeksPerPeriod;
        return (totalWeeks + sessionsPerPeriod - 1) / sessionsPerPeriod;
    }

    // O prazo para completar as sessões cabe no máximo de 52 semanas?
    public boolean fitsMaxTerm(int sessionCount) {
        return weeksToComplete(sessionCount) <= MAX_TERM_WEEKS;
    }
}
