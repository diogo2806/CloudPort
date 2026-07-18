# syntax=docker/dockerfile:1.7

# Este Dockerfile usa /backend como contexto de build.
# EasyPanel: Caminho de Build = /backend; Arquivo = Dockerfile.
FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY . .

WORKDIR /workspace/cloudport-modules

# Instala o agregador necessário para resolver os módulos do runtime.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -N -f ../cloudport-navio-modules/pom.xml -DskipTests install

# Barreira obrigatória do deploy: compila e executa todos os testes do backend.
# Remove POMs corrompidos por respostas HTML preservadas no cache do BuildKit.
# Qualquer falha retorna código diferente de zero e interrompe o build do EasyPanel.
RUN --mount=type=cache,target=/root/.m2 \
    find /root/.m2/repository -type f -name '*.pom' -exec sh -c \
        'grep -Eqi "<html|<!doctype html" "$1" && rm -f "$1" || true' _ {} \; && \
    mvn -B \
        -Dmaven.test.skip=false \
        -DskipTests=false \
        -Dspring-boot.repackage.skip=true \
        -pl :cloudport-runtime -am \
        clean install

# Os testes já foram aprovados acima; esta etapa gera somente o JAR executável final.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -pl :cloudport-runtime -DskipTests package

FROM eclipse-temurin:17-jre

WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 --home-dir /nonexistent --shell /usr/sbin/nologin cloudport \
    && mkdir -p /var/lib/cloudport/documents \
    && chown -R cloudport:cloudport /app /var/lib/cloudport

COPY --from=build /workspace/cloudport-runtime/target/cloudport-runtime-*.jar /app/app.jar
COPY cloudport-runtime/docker/entrypoint.sh /app/entrypoint.sh

USER cloudport
EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=120s --retries=20 \
    CMD curl -fsS http://127.0.0.1:8080/actuator/health/readiness || exit 1

ENTRYPOINT ["sh", "/app/entrypoint.sh"]
