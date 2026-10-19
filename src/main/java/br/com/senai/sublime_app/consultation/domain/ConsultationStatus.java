package br.com.senai.sublime_app.consultation.domain;

// Por que a sessão foi cobrada. Só existem os status que geram cobrança e repasse:
// o prestador lança apenas o que vai receber. Desmarcação dentro do prazo e
// cancelamento pela clínica não são lançados (ficam no software de agendamento).
// Ver domain-model.md, "Status: só o que é cobrado e gera repasse".
public enum ConsultationStatus {

    // O atendimento aconteceu
    ATTENDED,

    // O paciente faltou sem nenhum aviso
    NO_SHOW,

    // O paciente desmarcou fora do prazo mínimo de 6h de antecedência (ex: 3h, 15min antes).
    // A regra das 6h é aplicada por quem lança — o sistema não guarda hora do aviso.
    CANCELED_LATE
}
