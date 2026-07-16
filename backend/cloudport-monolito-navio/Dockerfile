# Execute a partir da raiz do repositorio:
# docker build -f backend/cloudport-monolito-navio/Dockerfile -t cloudport-monolito-navio .
FROM node:22-alpine AS frontend-build
WORKDIR /workspace/frontend/servico-navio-siderurgico
COPY frontend/servico-navio-siderurgico/package.json ./
RUN npm install --no-audit --no-fund
COPY frontend/servico-navio-siderurgico/ ./
RUN npm run build

FROM maven:3.9.6-eclipse-temurin-17 AS backend-build
WORKDIR /workspace

COPY backend/cloudport-navio-modules/pom.xml cloudport-navio-modules/pom.xml
COPY backend/cloudport-monolito-navio/pom.xml cloudport-monolito-navio/pom.xml
COPY backend/cloudport-monolito-navio/src cloudport-monolito-navio/src
COPY backend/servico-navio/pom.xml servico-navio/pom.xml
COPY backend/servico-navio/src servico-navio/src
COPY backend/servico-navio-siderurgico/pom.xml servico-navio-siderurgico/pom.xml
COPY backend/servico-navio-siderurgico/src servico-navio-siderurgico/src
COPY --from=frontend-build /workspace/frontend/servico-navio-siderurgico/dist cloudport-monolito-navio/src/main/resources/static

WORKDIR /workspace/cloudport-navio-modules
RUN mvn -B -Pmodulo-monolito -pl :cloudport-monolito-navio -am -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=backend-build /workspace/cloudport-monolito-navio/target/cloudport-monolito-navio-*.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
