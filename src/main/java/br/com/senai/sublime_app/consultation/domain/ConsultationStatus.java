package br.com.senai.sublime_app.consultation.domain;

public enum ConsultationStatus {

    // Faltou sem avisar
    MISSED,

    // Desmarcou com antecedência suficiente (sem cobrança)
    UNSCHEDULED_WITH_NOTICE,

    // Desmarcou fora do prazo (com cobrança)
    UNSCHEDULED_WITH_CHARGE,

    // Consulta cancelada
    CANCELED,

    // Compareceu e a consulta aconteceu
    ATTENDED
}