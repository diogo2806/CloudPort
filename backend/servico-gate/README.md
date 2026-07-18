# Módulo Gate

> Estado de transição: `servico-gate` ainda pode ser executado como aplicação Spring Boot independente, mas a arquitetura alvo do CloudPort é um monólito modular. Novas funcionalidades internas não devem ampliar o acoplamento distribuído deste deployment.

A decisão arquitetural e as regras de migração estão em [`../../docs/arquitetura-monolito-modular.md`](../../docs/arquitetura-monolito-modular.md).

## Responsabilidade

O módulo Gate concentra as integrações e operações de gate do CloudPort. Atualmente ele:

- expõe APIs REST de gate;
- processa a validação básica de imagens OCR localmente, no mesmo processo;
- integra com TOS e storage de documentos;
- valida autenticação e autorização do modelo legado;
- persiste seus dados no PostgreSQL configurado para o deployment.

Não existe dependência operacional de RabbitMQ no Gate. Mensageria não deve ser adicionada para fluxos internos ou para integrações sem consumidor real.

A integração de barcode com DMT permanece desabilitada. Enquanto não existir cliente DMT implementado e contratado, `GATE_BARCODE_HABILITADO` deve permanecer `false`; a aplicação impede a ativação de um fluxo que não possa entregar a solicitação.

Após a incorporação ao runtime monolítico, os contratos REST externos devem ser preservados, enquanto chamadas para outros módulos CloudPort devem migrar para portas locais.

## Pré-requisitos do deployment legado

- JDK 17;
- Maven 3.8+;
- PostgreSQL;
- variáveis de ambiente das integrações externas realmente utilizadas.

## Configuração

Configure as variáveis `GATE_*`, `TOS_API_*` e `DOCUMENT_STORAGE_*` no ambiente de execução. Garanta que o banco configurado exista e esteja acessível.

A recuperação periódica de OCR e a reconciliação noturna de barcode usam o controle canônico `cloudport.runtime.jobs-enabled`, exposto pela variável `CLOUDPORT_JOBS_ENABLED`. A ausência da variável mantém os jobs desabilitados. Somente o deployment responsável pelo processamento persistente deve configurar `CLOUDPORT_JOBS_ENABLED=true`; deployments standalone, legados ou de rollback permanecem fail-closed sem configuração adicional. Chamadas explícitas e submissões imediatas continuam disponíveis quando os agendamentos periódicos estão desabilitados.

Quando houver mais de uma réplica com jobs habilitados, o ciclo de barcode é serializado por advisory lock no PostgreSQL. Cada alerta é reivindicado individualmente com token e lease persistidos, e reutiliza a mesma chave de idempotência em restart ou retry. `GATE_RECONCILIACAO_ALERTA_LEASE` define o prazo para recuperar uma reivindicação interrompida e usa `PT2M` por padrão.

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

A execução isolada é um mecanismo de compatibilidade e rollback. O roteamento de produção deve possuir uma única origem ativa para cada rota, evitando que o deployment legado e o monólito processem a mesma escrita. Durante coexistência com o runtime canônico, não configure `CLOUDPORT_JOBS_ENABLED` no Gate legado; o valor padrão `false` impede a execução concorrente. A variável deve ser definida como `true` somente no deployment que assumirá os jobs persistentes.

## Testes

```bash
mvn test
```

## Regras para a migração

- manter controllers e contratos REST estáveis;
- mover regras de comunicação com Yard, autenticação e demais domínios para portas internas;
- manter somente integrações externas que possuam consumidor, contrato e necessidade operacional comprovados;
- executar processamento local diretamente quando produtor e consumidor pertencem ao mesmo módulo;
- não compartilhar entidades JPA ou repositories com outro módulo;
- desativar jobs e consumidores duplicados antes do corte de ambiente;
- remover este deployment somente após validar paridade, segurança, dados, observabilidade e rollback.
