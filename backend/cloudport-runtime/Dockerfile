# syntax=docker/dockerfile:1.7

# Execute a partir da raiz do repositorio:
# docker build -f backend/cloudport-runtime/Dockerfile -t cloudport-runtime .
FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY backend/ .

# Impede que o runtime seja empacotado com duas migrations da mesma versao.
# A verificacao permanece ativa mesmo quando os testes Maven sao ignorados no deploy.
RUN sh ./scripts/validar-versoes-flyway.sh

WORKDIR /workspace/cloudport-modules

# Instala o agregador necessário para resolver os módulos do runtime.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -N -f ../cloudport-navio-modules/pom.xml -Dmaven.test.skip=true install

# O build da imagem apenas compila e empacota o backend. Os testes permanecem na CI.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B \
        -Dmaven.test.skip=true \
        -Dspring-boot.repackage.skip=true \
        -pl :cloudport-runtime -am \
        clean install

# Gera somente o JAR executável final, sem compilar ou executar testes.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -Dmaven.test.skip=true -pl :cloudport-runtime package

FROM eclipse-temurin:17-jre

WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl netcat-openbsd \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 --home-dir /nonexistent --shell /usr/sbin/nologin cloudport \
    && mkdir -p /var/lib/cloudport/documents \
    && chown -R cloudport:cloudport /app /var/lib/cloudport

COPY --from=build /workspace/cloudport-runtime/target/cloudport-runtime-*.jar /app/app.jar
COPY backend/cloudport-runtime/docker/entrypoint.sh /app/entrypoint.sh

USER cloudport
EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=120s --retries=20 \
    CMD curl -fsS http://127.0.0.1:8080/actuator/health/readiness || exit 1

ENTRYPOINT ["sh", "/app/entrypoint.sh"]
