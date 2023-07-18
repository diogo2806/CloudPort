FROM gitpod/workspace-postgres

USER gitpod

RUN sudo apt-get update && sudo apt-get install -y postgresql postgresql-contrib

# Inicia o serviço PostgreSQL
RUN sudo service postgresql start &&\
    sudo -u postgres psql -c "CREATE DATABASE servico_autenticacao;" &&\
    sudo -u postgres psql -c "CREATE USER admin WITH SUPERUSER PASSWORD 'admin';" &&\
    sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE servico_autenticacao TO gitpod;"

ENV DATABASE_URL=postgres://gitpod:senha@localhost:5432/servico_autenticacao
