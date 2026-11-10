# syntax=docker/dockerfile:1
# Multi-stage build for Lume Monolith API (Java 25 + Spring Boot)

# ==============================================================================
# Estágio 1: Build da aplicação via Maven
# ==============================================================================
FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /workspace

# 1. Cache de dependências do Maven
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# 2. Copia o código-fonte e especificações da API (OpenAPI)
COPY src ./src

# 3. Compilação e empacotamento (ignora testes para o build do container)
RUN mvn clean package -DskipTests && \
    cp target/sboot-lume-monolith-api-*.jar target/app.jar

# ==============================================================================
# Estágio 2: Imagem final de execução (Runtime leve)
# ==============================================================================
FROM eclipse-temurin:25-jre
WORKDIR /app

# Cria usuário não-root para conformidade e segurança
RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

# Copia o artefato executável gerado
COPY --from=builder --chown=spring:spring /workspace/target/app.jar app.jar

# Portas documentadas (o Render utiliza a variável $PORT dinamicamente)
EXPOSE 8080 10000

# Otimizações da JVM para o plano Free do Render (512 MB de RAM):
# - UseSerialGC: Coletor de lixo com mínimo overhead de memória/threads
# - UseCompactObjectHeaders: Recurso do Java 25 que reduz o tamanho dos objetos na memória
# - MaxRAMPercentage=60.0: Mantém o heap contido em ~300MB, reservando espaço para Metaspace e stacks
# - Xss512k: Reduz footprint por thread
ENV JAVA_OPTS="-XX:+UseSerialGC -XX:+UseCompactObjectHeaders -XX:MaxRAMPercentage=60.0 -Xss512k -Dfile.encoding=UTF-8"

# Inicia a aplicação injetando a porta do Render ($PORT) com fallback para 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar --server.port=${PORT:-8080}"]
