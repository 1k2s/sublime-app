import { useRouteError, isRouteErrorResponse, Link } from 'react-router-dom';
import styles from './RouteError.module.css';

export function RouteError() {
  const error = useRouteError();
  
  let title = "Ocorreu um erro";
  let message = "Algo deu errado ao carregar esta página.";

  if (isRouteErrorResponse(error)) {
    if (error.status === 404) {
      title = "Página não encontrada";
      message = "A página que você está procurando não existe.";
    } else {
      title = `Erro ${error.status}`;
      message = error.statusText || error.data?.message || message;
    }
  } else if (error instanceof Error) {
    message = error.message;
  }

  return (
    <div className={styles.page}>
      <h1>{title}</h1>
      <p>{message}</p>
      <Link to="/" className={styles.home}>
        Voltar para o início
      </Link>
    </div>
  );
}
