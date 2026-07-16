# Execute a partir da raiz do repositorio:
# docker build -f backend/cloudport-runtime/Dockerfile -t cloudport-runtime .
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY backend/cloudport-modules/pom.xml cloudport-modules/pom.xml
COPY backend/cloudport-runtime/pom.xml cloudport-runtime/pom.xml
COPY backend/cloudport-runtime/src cloudport-runtime/src
COPY backend/servico-autenticacao servico-autenticacao
COPY backend/servico-gate servico-gate
COPY backend/servico-rail servico-rail
COPY backend/servico-visibilidade servico-visibilidade
COPY backend/servico-yard servico-yard
COPY backend/servico-navio servico-navio
COPY backend/servico-navio-siderurgico servico-navio-siderurgico

WORKDIR /workspace/cloudport-modules
RUN mvn -B -Dspring-boot.repackage.skip=true \
    -pl :cloudport-runtime -am \
    -DskipTests install && \
    mvn -B -pl :cloudport-runtime -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 10001 cloudport
COPY --from=build /workspace/cloudport-runtime/target/cloudport-runtime-*.jar app.jar
USER cloudport
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
