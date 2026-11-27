# 0053 — A homologação: o Caddy como porteiro, o front dentro dele e as imagens no GHCR

- **Status:** aceito
- **Data:** 2026-11-27

## Contexto

Antes do MVP (a produção da clínica), o time precisa de um ambiente no ar, igual ao de produção,
para testar pela tela e treinar o fluxo de deploy de um projeto real. O ambiente roda numa VPS
pequena (1 vCPU, 4 GB) e o repositório é público: o código e o que sair dele podem ser lidos por
qualquer pessoa.

## Decisão

- **Tudo em containers**, num `docker-compose` (`deploy/homologacao/`): o porteiro (`web`), o
  `backend` e o `mysql`. **Só o porteiro publica portas** (80 e 443); o backend e o banco ficam na
  rede interna do Docker.
- **O Caddy é o porteiro:** emite e renova sozinho o certificado HTTPS (Let's Encrypt). O **front
  buildado vai dentro da imagem dele**: um serviço a menos, e o front e a API no mesmo endereço, sem
  CORS. O backend lê o `X-Forwarded-Proto` (`SERVER_FORWARD_HEADERS_STRATEGY=native`) para saber
  que a chamada veio por HTTPS.
- **A configuração por variável de ambiente:** o mesmo `application.properties` e as mesmas imagens
  servem a qualquer ambiente; os segredos ficam só no `.env` do servidor. O endereço do site também
  vem do ambiente (`SITE_ADDRESS`), e a mesma imagem `web` serve à homologação e, depois, à
  produção.
- **As imagens no registro do GitHub (GHCR), públicas:** o servidor só baixa e sobe, sem o código e
  sem compilar. **Nenhum segredo entra numa imagem.**
- **O MySQL com um usuário próprio da aplicação** (não o `root`), a memória reduzida (sem o
  *performance schema*, o cache do InnoDB em 256 MB) e limites de memória por container (o backend
  com a JVM em 70% do limite).

## Alternativas descartadas

- **O nginx:** faz o mesmo papel, mas o HTTPS fica por nossa conta (o certbot e a renovação).
- **O front num container próprio:** mais um serviço para subir e atualizar, sem ganho; o front é só
  um punhado de arquivos estáticos, e quem os serve já é o porteiro.
- **Compilar no servidor** (clonar e buildar na VPS): lento com 1 vCPU e exige o código lá; o build
  fica no CI.
- **Um perfil do Spring para a homologação** (`application-homolog.properties`): eram poucas
  diferenças, todas resolvidas por variável de ambiente.
- **Liberar a origem da homologação no CORS:** trataria o sintoma (o 403 do login pelo navegador);
  o certo é o backend saber que está atrás de um porteiro HTTPS.

## Consequências

- Publicar uma versão é montar as imagens, dar `push` e, na VPS, `pull` e `up` (ver "Homologação
  (VPS)" no [`development.md`](../development.md)). Até o CI/CD, isso é à mão.
- Uma porta publicada por engano abre o serviço para a internet, porque o Docker passa por fora do
  firewall: a regra fica comentada no `docker-compose.yml`.
- O MVP usa o mesmo desenho, com outro `.env` e outro endereço.
