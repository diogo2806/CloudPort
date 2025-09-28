# CloudPort

CloudPort é um Terminal Operating System (TOS) inovador, construído sobre a arquitetura de microsserviços.

## Serviço de Autenticação

Este repositório hospeda o serviço de autenticação do CloudPort, que é encarregado de autenticar e autorizar usuários. O projeto foi inicializado com o Spring Initializr, uma ferramenta que agiliza a criação de aplicações Spring.

### Dependências

O projeto depende das seguintes bibliotecas e ferramentas:

- **Spring Web**: Usado para construir aplicações web, incluindo serviços RESTful, com Spring MVC.
- **Spring Data JPA**: Facilita a criação de repositórios orientados a dados.
- **Spring Security**: Fornece recursos de segurança robustos, incluindo autenticação e autorização.
- **PostgreSQL Driver**: Permite a conexão com PostgreSQL, um sistema de banco de dados objeto-relacional.
- **Spring Boot DevTools**: Oferece recursos de desenvolvimento úteis, como atualização automática.
- **Lombok**: Uma biblioteca que ajuda a reduzir o código boilerplate em Java.
- **Spring Boot Validation**: Fornece suporte para validação de dados.
- **Flyway Migration**: Uma ferramenta para migração de base de dados.
- **Spring Security OAuth2 Client**: Facilita a criação de aplicações que são clientes de provedores OAuth 2.0.
- **Spring Security OAuth2 Resource Server**: Facilita a criação de aplicações que são servidores de recursos OAuth 2.0.
- **UUID como identificador de usuário**: O serviço de autenticação utiliza UUIDs para identificar usuários de forma única.

- **Download para iniciar o microsserviço**:
[Link para o Spring Initializr](https://start.spring.io/#!type=maven-project&language=java&platformVersion=3.1.1&packaging=jar&jvmVersion=11&groupId=br.com.cloudport&artifactId=servico-autenticacao&name=servico-autenticacao&description=Servi%C3%A7o%20respons%C3%A1vel%20pela%20autentica%C3%A7%C3%A3o%20e%20autoriza%C3%A7%C3%A3o%20de%20usu%C3%A1rios%20na%20aplica%C3%A7%C3%A3o%20CloudPort.&packageName=br.com.cloudport.servico-autenticacao&dependencies=web,data-jpa,security,postgresql,devtools,lombok,validation,flyway,oauth2-client,oauth2-resource-server)

### Como Rodar o Projeto

1. Clone o projeto para o seu ambiente local.
2. Certifique-se de que você tem o Maven e o JDK 17 instalados.
3. Navegue até a raiz do projeto via linha de comando.
4. Copie o arquivo `env.example` para `.env` e ajuste as variáveis de ambiente de acordo com sua infraestrutura (RabbitMQ, PostgreSQL e chaves JWT).
5. Exporte as variáveis definidas no `.env` para o seu shell (`export $(grep -v '^#' .env | xargs)` em ambientes Unix) ou configure-as no serviço de execução da aplicação.
6. Execute `createdb servico_autenticacao`.
7. Execute `mvn spring-boot:run`.

### Contribuição

Contribuições são sempre bem-vindas. Se você deseja contribuir, por favor, abra uma issue primeiro para discutir o que você gostaria de mudar.


## Serviço de Gestão de Pátio

O microserviço **servico-yard** é um exemplo simples de gestão de contêineres no pátio. Ele expõe duas rotas REST:

- `GET /yard/containers` – lista os contêineres registrados.
- `POST /yard/containers` – adiciona um novo contêiner.

Para executá-lo, navegue até `backend/servico-yard` e rode `mvn spring-boot:run`. O serviço inicia na porta `8081`.

## Serviço de Gate

O serviço **servico-gate** centraliza integrações de gate, realizando chamadas ao TOS, comunicação via RabbitMQ e persistência em PostgreSQL. Para executá-lo, configure as variáveis `GATE_*`, `TOS_API_*` e `DOCUMENT_STORAGE_*` descritas em `env.example`. Certifique-se também de que PostgreSQL e RabbitMQ estejam operacionais.

### Subindo o serviço manualmente

```bash
cd backend/servico-gate
mvn spring-boot:run
```

Por padrão o serviço expõe a porta `GATE_SERVER_PORT` (valor padrão `8082`). Ajuste-a conforme necessário quando executar múltiplos serviços localmente.

### Dependências externas sugeridas via Docker Compose

Um arquivo `docker-compose` não está incluído, mas recomenda-se preparar containers semelhantes aos exemplos abaixo para desenvolvimento local:

```yaml
services:
  gate-postgres:
    image: postgres:13
    ports:
      - "5433:5432"
    environment:
      POSTGRES_DB: servico_gate
      POSTGRES_USER: ${GATE_DB_USERNAME:-postgres}
      POSTGRES_PASSWORD: ${GATE_DB_PASSWORD:-postgres}
  gate-rabbitmq:
    image: rabbitmq:3-management
    ports:
      - "5672:5672"
      - "15672:15672"
    environment:
      RABBITMQ_DEFAULT_USER: ${GATE_RABBIT_USERNAME:-guest}
      RABBITMQ_DEFAULT_PASS: ${GATE_RABBIT_PASSWORD:-guest}
```

Atualize as portas caso já existam instâncias locais em execução.
