FROM gitpod/workspace-java-17

# Install PostgreSQL
USER root
RUN apt-get update && apt-get install -y postgresql
USER gitpod

# Start PostgreSQL service
RUN sudo service postgresql start
