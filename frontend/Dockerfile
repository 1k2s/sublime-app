# Imagem "web": o front buildado servido pelo Caddy, que também é o porteiro da aplicação (o
# HTTPS e o /api para o backend; ver o Caddyfile). Dois estágios: o primeiro builda o React com o
# Node; o segundo é só o Caddy com os arquivos estáticos (sem o Node nem o node_modules).

# --- Estágio 1: o build do React ---
FROM node:22-alpine AS build
WORKDIR /app

# Primeiro só as dependências: a camada do npm ci é reaproveitada enquanto o package-lock não mudar
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
# A API no mesmo endereço do site (o Caddy encaminha o /api): um caminho relativo, que serve a
# qualquer ambiente sem buildar de novo. O Vite grava o valor no JavaScript na hora do build
ARG VITE_API_URL=/api
ENV VITE_API_URL=$VITE_API_URL
RUN npm run build

# --- Estágio 2: o Caddy com o front ---
FROM caddy:2.10
# Liga a imagem publicada no registro (GHCR) ao repositório: o pacote aparece nele e herda as
# permissões dele
LABEL org.opencontainers.image.source=https://github.com/1k2s/sublime-app
COPY Caddyfile /etc/caddy/Caddyfile
COPY --from=build /app/dist /srv/frontend
