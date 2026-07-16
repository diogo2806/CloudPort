# Módulo Gate

> Estado de transição: `servico-gate` ainda pode ser executado como aplicação Spring Boot independente, mas a arquitetura alvo do CloudPort é um monólito modular. Novas funcionalidades internas não devem ampliar o acoplamento distribuído deste deployment.

A decisão arquitetural e as regras de migração estão em [`../../docs/arquitetura-monolito-modular.md`](../../docs/arquitetura-monolito-modular.md).

## Responsabilidade

O módulo Gate concentra as integrações e operações de gate do CloudPort. Atualmente ele:

- expõe APIs REST de gate;
- troca mensagens com integrações assíncronas por RabbitMQ;
- integra com TOS e storage de documentos;
- valida autenticação e autorização do modelo legado;
- persiste seus dados no PostgreSQL configurado para o deployment.

Após a incorporação ao runtime monolítico, os contratos REST externos devem ser preservados, enquanto chamadas para outros módulos CloudPort devem migrar para portas locais.

## Pré-requisitos do deployment legado

- JDK 11 ou superior;
- Maven 3.8+;
- PostgreSQL;
- RabbitMQ para os fluxos que utilizam mensageria;
- variáveis de ambiente das integrações externas.

## Configuração

Configure as variáveis `GATE_*`, `TOS_API_*` e `DOCUMENT_STORAGE_*` no ambiente de execução. Garanta que o banco indicado em `GATE_DB_URL` exista e esteja acessível.

Exemplo de criação de banco para desenvolvimento:

```bash
createdb servico_gate
```

## Execução isolada durante a transição

```bash
cd backend/servico-gate
mvn spring-boot:run
```

A porta padrão é `8082`, podendo ser alterada por `GATE_SERVER_PORT`.

A execução isolada é um mecanismo de compatibilidade e rollback. O roteamento de produção deve possuir uma única origem ativa para cada rota, evitando que o deployment legado e o monólito processem a mesma escrita.

## Testes

```bash
mvn test
```

## Regras para a migração

- manter controllers e contratos REST estáveis;
- mover regras de comunicação com Yard, autenticação e demais domínios para portas internas;
- manter TOS, OCR, documentos e mensageria como adaptadores externos;
- não compartilhar entidades JPA ou repositories com outro módulo;
- desativar jobs e consumidores duplicados antes do corte de ambiente;
- remover este deployment somente após validar paridade, segurança, dados, observabilidade e rollback.
