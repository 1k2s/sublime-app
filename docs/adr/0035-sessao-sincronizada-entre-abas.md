# 0035 — Uma sessão por navegador, com as abas sincronizadas pelo evento `storage`

- **Status:** aceito
- **Data:** 2026-11-15

## Contexto

A sessão fica no `localStorage` ([ADR 0024](0024-sessao-frontend-localstorage.md)), que todas as
abas do navegador dividem, mas o usuário mostrado pela tela fica no estado do React de cada aba,
lido uma vez, ao abrir. Quando outra aba saía e entrava com outro usuário, a aba antiga continuava
com o menu e as telas do usuário anterior, enquanto as requisições levavam o token do novo (o
`apiClient` lê o `localStorage` a cada uma). Ex: a aba do admin recebia 403 ao inativar um
prestador; e, no sentido inverso, a tela do prestador agia com o token do admin. O backend
continuava correto (vale o token), mas a tela mentia sobre quem estava agindo. Notado nos testes de
2026-11-13.

## Decisão

- **Uma sessão por navegador:** dois usuários em abas do mesmo navegador não é possível; vale o
  último login.
- **As abas acompanham a sessão:** o `AuthProvider` ouve o evento `storage`, que o navegador
  dispara nas outras abas quando uma delas muda o `localStorage`. A aba relê a sessão inteira:
  outra aba saiu → volta para o login; entrou com outro usuário → limpa o cache do TanStack Query
  e assume o novo usuário (menu e rotas do perfil dele); o mesmo usuário mudou o e-mail → só
  atualiza o menu.
- **A releitura só lê, nunca apaga.** O login grava três chaves em sequência; no meio dele, a outra
  aba vê a sessão incompleta, e apagá-la levaria o token recém-gravado.

## Alternativas descartadas

- **Uma sessão por aba (`sessionStorage`):** permitiria dois perfis lado a lado, mas toda aba nova
  (Ctrl+clique, abrir o sistema de novo) pediria login, e fechar a aba deslogaria. O ganho só
  serve a quem testa, e uma janela anônima ou outro navegador já resolve.
- **Limpar a sessão antes de cada login:** a limpeza acontece na aba que entra; a aba antiga não
  fica sabendo e segue com o usuário anterior na tela.
- **Conferir a sessão só ao abrir a aba** (como era): a aba nova já reaproveita a sessão; o
  problema é a aba que já estava aberta.

## Consequências

- Ver "Rotas e sessão" na seção "Frontend" do `architecture.md`.
- A troca de usuário numa aba é silenciosa: ela muda de tela sem aviso.
- Para testar dois perfis ao mesmo tempo: outro navegador ou uma janela anônima.
