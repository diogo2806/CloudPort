# syntax=docker/dockerfile:1.7

# Este Dockerfile usa /backend como contexto de build.
# EasyPanel: Caminho de Build = /backend; Arquivo = Dockerfile.
FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY . .

# Impede que o runtime seja empacotado com duas migrations da mesma versao.
# A verificacao permanece ativa mesmo quando os testes Maven sao ignorados no deploy.
RUN sh ./scripts/validar-versoes-flyway.sh

WORKDIR /workspace/cloudport-modules

# O cache identificado é compartilhado entre os passos e builds do backend.
# sharing=locked evita que dois builds concorrentes corrompam o repositório Maven.
RUN --mount=type=cache,id=cloudport-maven-repository,target=/root/.m2,sharing=locked \
    mvn -B -N -f ../cloudport-navio-modules/pom.xml -Dmaven.test.skip=true install

# O deploy apenas compila e empacota o backend. Os testes continuam executados pela CI.
# Remove do cache qualquer POM inválido deixado por downloads anteriores.
RUN --mount=type=cache,id=cloudport-maven-repository,target=/root/.m2,sharing=locked \
    find /root/.m2/repository -type f -name '*.pom' -exec sh -c \
        'grep -Eqi "<html|<!doctype html" "$1" && rm -f "$1" || true' _ {} \; && \
    mvn -B \
        -Dmaven.test.skip=true \
        -Dspring-boot.repackage.skip=true \
        -pl :cloudport-runtime -am \
        clean install

# Gera somente o JAR executável final, sem compilar ou executar testes.
RUN --mount=type=cache,id=cloudport-maven-repository,target=/root/.m2,sharing=locked \
    mvn -B -Dmaven.test.skip=true -pl :cloudport-runtime package

FROM eclipse-temurin:17-jre

LABEL org.opencontainers.image.title="CloudPort Backend" \
      org.opencontainers.image.source="https://github.com/diogo2806/CloudPort"

WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl netcat-openbsd \
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