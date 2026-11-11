// Mensagem no lugar de uma lista ou tabela, na ordem: carregando, erro da API, o filtro escondeu
// todos os registros (hasRecords) ou nada cadastrado. Cada tela passa os textos com o nome do
// que lista (messages: loading, error, filtered, empty, emptyHint)
import { Card } from "./Card";
import styles from "./ListMessage.module.css";

function pickMessage({ isLoading, isError, hasRecords, messages }) {
  if (isLoading) return { text: messages.loading };
  if (isError) return { title: messages.error, text: "Tente novamente em instantes." };
  if (hasRecords) return { title: messages.filtered, text: "Tente ajustar os filtros." };
  return { title: messages.empty, text: messages.emptyHint };
}

export function ListMessage({ isLoading = false, isError = false, hasRecords = false, messages, className }) {
  const message = pickMessage({ isLoading, isError, hasRecords, messages });

  return (
    <Card className={[styles.card, className].filter(Boolean).join(" ")}>
      {message.title && <p className={styles.title}>{message.title}</p>}
      {message.text && <span className={styles.text}>{message.text}</span>}
    </Card>
  );
}
