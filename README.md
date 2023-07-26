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
- **Lombok**: Uma biblioteca que ajuda a reduzir o código boilerplate em Kotlin.
- **Spring Boot Validation**: Fornece suporte para validação de dados.
- **Flyway Migration**: Uma ferramenta para migração de base de dados.
- **Spring Security OAuth2 Client**: Facilita a criação de aplicações que são clientes de provedores OAuth 2.0.
- **Spring Security OAuth2 Resource Server**: Facilita a criação de aplicações que são servidores de recursos OAuth 2.0.

- **Dowload para iniciar o microsserviço**:
[Link para o Spring Initializr](https://start.spring.io/#!type=maven-project&language=java&platformVersion=3.1.1&packaging=jar&jvmVersion=11&groupId=br.com.cloudport&artifactId=servico-autenticacao&name=servico-autenticacao&description=Servi%C3%A7o%20respons%C3%A1vel%20pela%20autentica%C3%A7%C3%A3o%20e%20autoriza%C3%A7%C3%A3o%20de%20usu%C3%A1rios%20na%20aplica%C3%A7%C3%A3o%20CloudPort.&packageName=br.com.cloudport.servico-autenticacao&dependencies=web,data-jpa,security,postgresql,devtools,lombok,validation,flyway,oauth2-client,oauth2-resource-server)

### Como Rodar o Projeto

1. Clone o projeto para o seu ambiente local.
2. Certifique-se de que você tem o Maven e o JDK 17 instalados.
3. Navegue até a raiz do projeto via linha de comando.
4. Execute `createdb servico_autenticacao`.
5. Execute `mvn spring-boot:run`.

### Contribuição

Contribuições são sempre bem-vindas. Se você deseja contribuir, por favor, abra uma issue primeiro para discutir o que você gostaria de mudar.

createdb servico_autenticacao
mvn spring-boot:run
