# Execute a partir da pasta backend:
# docker build -f cloudport-monolito-navio/Dockerfile -t cloudport-monolito-navio .
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY cloudport-monolito-navio/pom.xml cloudport-monolito-navio/pom.xml
COPY cloudport-monolito-navio/src cloudport-monolito-navio/src
COPY servico-navio/src/main/java servico-navio/src/main/java
COPY servico-navio-siderurgico/src/main/java servico-navio-siderurgico/src/main/java

WORKDIR /workspace/cloudport-monolito-navio
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/cloudport-monolito-navio/target/cloudport-monolito-navio-*.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
