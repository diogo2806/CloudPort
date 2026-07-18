# syntax=docker/dockerfile:1.7

# Este Dockerfile usa /backend como contexto de build.
# EasyPanel: Caminho de Build = /backend; Arquivo = Dockerfile.
FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY . .

WORKDIR /workspace/cloudport-modules
RUN mvn -B -N -f ../cloudport-navio-modules/pom.xml -DskipTests install \
    && mvn -B -Dspring-boot.repackage.skip=true \
        -pl :cloudport-runtime -am \
        -DskipTests install \
    && mvn -B -pl :cloudport-runtime -DskipTests clean package \
    && mvn -B -pl :cloudport-runtime \
        -Dtest=FlywayMigrationPackagingUniquenessTest test

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
