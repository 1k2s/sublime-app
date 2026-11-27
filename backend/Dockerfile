# Imagem do backend (a homologação e, depois, a produção). Dois estágios: o primeiro compila
# com o JDK; o segundo só roda, com o JRE, sem o Maven nem o código-fonte (imagem menor e com
# menos coisa para atacar). Os testes rodam no CI, não aqui.

# --- Estágio 1: o build ---
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Primeiro só o Maven e o pom: as dependências baixadas ficam numa camada própria, que o Docker
# reaproveita enquanto o pom.xml não mudar (o build seguinte não baixa tudo de novo)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -q dependency:go-offline

# Depois o código, que muda a cada commit
COPY src/ src/
RUN ./mvnw -q -DskipTests package && cp target/sublime-app-*.jar app.jar

# --- Estágio 2: a execução ---
FROM eclipse-temurin:25-jre
# Liga a imagem publicada no registro (GHCR) ao repositório: o pacote aparece nele e herda as
# permissões dele
LABEL org.opencontainers.image.source=https://github.com/1k2s/sublime-app
WORKDIR /app

# A aplicação roda com um usuário sem privilégios, não como root dentro do container
RUN useradd --system --uid 1001 sublime
COPY --from=build /app/app.jar app.jar
USER sublime

EXPOSE 8080
# A JVM usa até 70% da memória do container (o limite vem do docker-compose); o resto fica
# para a própria JVM fora do heap (threads, classes, buffers)
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70"
ENTRYPOINT ["java", "-jar", "app.jar"]
