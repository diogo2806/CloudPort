FROM postgres:latest
ENV POSTGRES_PASSWORD=gitpod
ENV POSTGRES_USER=gitpod
ENV POSTGRES_DB=servico_autenticacao
EXPOSE 5433
