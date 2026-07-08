FROM node:22-alpine AS frontend-build
WORKDIR /workspace/frontend/servico-navio-siderurgico
COPY frontend/servico-navio-siderurgico/package*.json ./
RUN npm install
COPY frontend/servico-navio-siderurgico/ ./
RUN npm run build

FROM maven:3.9.6-eclipse-temurin-17 AS backend-build
WORKDIR /workspace/backend/servico-navio-siderurgico
COPY backend/servico-navio-siderurgico/pom.xml ./
COPY backend/servico-navio-siderurgico/src ./src
COPY --from=frontend-build /workspace/frontend/servico-navio-siderurgico/dist/servico-navio-siderurgico ./src/main/resources/static
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=backend-build /workspace/backend/servico-navio-siderurgico/target/servico-navio-siderurgico-*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
