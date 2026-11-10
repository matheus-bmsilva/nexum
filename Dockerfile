# ==============================================================================
# Multi-stage Dockerfile para Nexum — Gestão para Arquitetos (Java 21)
# Otimizado para o plano gratuito do Render (baixo consumo de RAM e inicialização rápida)
# ==============================================================================

# Estágio 1: Build & Compilação
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copia bibliotecas e código-fonte
COPY lib/ ./lib/
COPY src/ ./src/

# Cria diretório de classes e compila com UTF-8
RUN mkdir -p build/classes && \
    javac -encoding UTF-8 -cp "lib/*" -d build/classes $(find src -name "*.java")

# Estágio 2: Imagem final enxuta de execução (JRE Alpine)
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Cria diretório para persistência do banco de dados SQLite
RUN mkdir -p database

# Copia apenas as classes compiladas, bibliotecas e frontend
COPY --from=builder /app/build/classes ./build/classes
COPY --from=builder /app/lib ./lib
COPY web/ ./web/

# Variáveis de ambiente padrão (sobrescritas automaticamente pelo Render)
ENV PORT=8080
ENV DATABASE_PATH=/app/database/nexum.db

# Porta exposta
EXPOSE 8080

# Inicialização da aplicação
CMD ["java", "-cp", "build/classes:lib/*", "nexum.Nexum"]
