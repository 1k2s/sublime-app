import { useRouteError, isRouteErrorResponse, Link } from 'react-router-dom';

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
    <div style={{ padding: '2rem', textAlign: 'center', fontFamily: 'system-ui, sans-serif' }}>
      <h1>{title}</h1>
      <p>{message}</p>
      <Link to="/" style={{ display: 'inline-block', marginTop: '1rem', padding: '0.5rem 1rem', background: '#5f7a58', color: '#ffffff', textDecoration: 'none', borderRadius: 'var(--radius, 12px)' }}>
        Voltar para o início
      </Link>
    </div>
  );
}
