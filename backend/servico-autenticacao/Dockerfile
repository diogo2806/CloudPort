# Usando a imagem oficial do Java 17
FROM openjdk:17-jdk-slim

# Váriável de ambiente para identificar o diretório de trabalho no container
ARG APP_HOME=/app

# Cria o diretório e define como o diretório de trabalho
RUN mkdir $APP_HOME
WORKDIR $APP_HOME

# Copia o arquivo de construção .jar para o diretório de trabalho no container
COPY target/*.jar $APP_HOME/servico-autenticacao.jar

# Configura o container para rodar o .jar quando o container iniciar
ENTRYPOINT ["java", "-jar", "servico-autenticacao.jar"]
