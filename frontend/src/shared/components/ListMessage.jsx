// Mensagem no lugar de uma lista ou tabela, na ordem: carregando (o PageLoader, com o texto de
// loading para o leitor de tela), erro da API, o filtro escondeu todos os registros (hasRecords)
// ou nada cadastrado. Cada tela passa os textos com o nome do que lista (messages: loading,
// error, filtered, empty, emptyHint)
import { Card } from "./Card";
import { PageLoader } from "./PageLoader";
import styles from "./ListMessage.module.css";

function pickMessage({ isError, hasRecords, messages }) {
  if (isError) return { title: messages.error, text: "Tente novamente em instantes." };
  if (hasRecords) return { title: messages.filtered, text: "Tente ajustar os filtros." };
  return { title: messages.empty, text: messages.emptyHint };
}

export function ListMessage({ isLoading = false, isError = false, hasRecords = false, messages, className }) {
  const cardClass = [styles.card, className].filter(Boolean).join(" ");

  if (isLoading) {
    return (
      <Card className={cardClass}>
        <PageLoader variant="section" label={messages.loading} />
      </Card>
    );
  }

  const message = pickMessage({ isError, hasRecords, messages });

  return (
    <Card className={cardClass}>
      {message.title && <p className={styles.title}>{message.title}</p>}
      {message.text && <span className={styles.text}>{message.text}</span>}
    </Card>
  );
}
