# Execute a partir da raiz do repositório:
# docker build -f backend/cloudport-monolito-navio/Dockerfile -t cloudport-monolito .
FROM node:22-alpine AS frontend-build
WORKDIR /workspace/frontend/servico-navio-siderurgico
COPY frontend/servico-navio-siderurgico/package.json ./
RUN npm install --no-audit --no-fund
COPY frontend/servico-navio-siderurgico/ ./
RUN npm run build

FROM maven:3.9.9-eclipse-temurin-17 AS backend-build
WORKDIR /workspace

COPY backend/cloudport-navio-modules/pom.xml cloudport-navio-modules/pom.xml
COPY backend/cloudport-contracts/pom.xml cloudport-contracts/pom.xml
COPY backend/cloudport-contracts/src cloudport-contracts/src
COPY backend/cloudport-monolito-navio/pom.xml cloudport-monolito-navio/pom.xml
COPY backend/cloudport-monolito-navio/src cloudport-monolito-navio/src
COPY backend/servico-navio/pom.xml servico-navio/pom.xml
COPY backend/servico-navio/src servico-navio/src
COPY backend/servico-navio-siderurgico/pom.xml servico-navio-siderurgico/pom.xml
COPY backend/servico-navio-siderurgico/src servico-navio-siderurgico/src
COPY backend/servico-yard/pom.xml servico-yard/pom.xml
COPY backend/servico-yard/src servico-yard/src
COPY backend/servico-gate/pom.xml servico-gate/pom.xml
COPY backend/servico-gate/src servico-gate/src
COPY backend/servico-rail/pom.xml servico-rail/pom.xml
COPY backend/servico-rail/src servico-rail/src
COPY backend/servico-autenticacao/pom.xml servico-autenticacao/pom.xml
COPY backend/servico-autenticacao/src servico-autenticacao/src
COPY backend/servico-visibilidade/pom.xml servico-visibilidade/pom.xml
COPY backend/servico-visibilidade/src servico-visibilidade/src
COPY --from=frontend-build /workspace/frontend/servico-navio-siderurgico/dist/servico-navio-siderurgico cloudport-monolito-navio/src/main/resources/static

WORKDIR /workspace/cloudport-navio-modules
RUN mvn -B -Pmodulo-monolito -pl :cloudport-monolito-navio -am -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=backend-build /workspace/cloudport-monolito-navio/target/cloudport-monolito-navio-*.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
