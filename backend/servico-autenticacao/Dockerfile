# Primeira etapa - Construir a aplicação com Maven
FROM maven:3.8.1-openjdk-17-slim AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package

# Segunda etapa - Copiar o JAR gerado na primeira etapa e executar o aplicativo
FROM openjdk:17-jdk-slim
ARG APP_HOME=/app
RUN mkdir $APP_HOME
WORKDIR $APP_HOME
COPY --from=build /app/target/*.jar $APP_HOME/servico-autenticacao.jar
ENTRYPOINT ["java", "-jar", "servico-autenticacao.jar"]
