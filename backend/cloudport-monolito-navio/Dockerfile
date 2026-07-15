# Execute a partir da pasta backend:
# docker build -f cloudport-monolito-navio/Dockerfile -t cloudport-monolito-navio .
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY cloudport-navio-modules/pom.xml cloudport-navio-modules/pom.xml
COPY cloudport-monolito-navio/pom.xml cloudport-monolito-navio/pom.xml
COPY cloudport-monolito-navio/src cloudport-monolito-navio/src
COPY servico-navio/pom.xml servico-navio/pom.xml
COPY servico-navio/src servico-navio/src
COPY servico-navio-siderurgico/pom.xml servico-navio-siderurgico/pom.xml
COPY servico-navio-siderurgico/src servico-navio-siderurgico/src

WORKDIR /workspace/cloudport-navio-modules
RUN mvn -B -Pmodulo-monolito -pl :cloudport-monolito-navio -am -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/cloudport-monolito-navio/target/cloudport-monolito-navio-*.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
