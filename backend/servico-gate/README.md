# Serviço Gate

O serviço **servico-gate** atua como orquestrador das integrações de gate do CloudPort. Ele expõe APIs REST, troca mensagens via RabbitMQ e se integra com o TOS, storage de documentos e serviços de autenticação.

- Visão completa da arquitetura: [`docs/servico-gate-architecture.md`](../../docs/servico-gate-architecture.md)
- Procedimentos operacionais padrão: [`docs/servico-gate-operacoes.md`](../../docs/servico-gate-operacoes.md)
- Scripts REST para testes manuais: [`tools/api/servico-gate.http`](../../tools/api/servico-gate.http)

## Pré-requisitos

- JDK 11 ou superior
- Maven 3.8+
- PostgreSQL com uma base dedicada
- Instância RabbitMQ acessível

## Configuração

1. Copie o arquivo `env.example` na raiz do projeto para `.env` e ajuste as variáveis que começam com `GATE_`, `TOS_API_` e `DOCUMENT_STORAGE_`.
2. Exporte as variáveis no terminal com `export $(grep -v '^#' .env | xargs)` ou configure-as no serviço de execução.
3. Garanta que o banco de dados configurado em `GATE_DB_URL` exista. Exemplo:
   ```bash
   createdb servico_gate
   ```

## Execução

Na raiz do projeto, execute o comando abaixo para iniciar o serviço:

```bash
cd backend/servico-gate
mvn spring-boot:run
```

Por padrão o serviço inicia na porta definida em `GATE_SERVER_PORT` (8082).

Para levantar a stack completa com PostgreSQL, RabbitMQ, Redis e mocks de integrações, utilize o [`docker-compose`](../../docker/docker-compose.yml):

```bash
cp env.example .env
docker compose -f docker/docker-compose.yml up --build
```

## Testes

Para rodar os testes automatizados:

```bash
mvn test
```
