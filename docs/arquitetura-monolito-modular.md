# Arquitetura do monólito modular CloudPort

## Status da decisão

- Estado: vigente.
- Arquitetura alvo: monólito modular.
- Runtime geral: `backend/cloudport-runtime`.
- Primeiro corte preservado para rollback: `backend/cloudport-monolito-navio`.
- Módulos incorporados: Autenticação, Carga Geral, Gate, Rail, Visibilidade, Yard, Navio e Navio Siderúrgico.

Este documento é a referência para estrutura, comunicação, persistência, segurança, build, implantação e rollback do backend.

## Decisão

O CloudPort executa suas funcionalidades internas em um único processo Spring Boot, mantendo limites explícitos entre os módulos de negócio.

O runtime geral possui:

1. um artefato e um processo para o backend;
2. módulos Maven com responsabilidade e dependências explícitas;
3. comunicação local por portas, serviços de aplicação e eventos internos;
4. contratos HTTP preservados na borda para frontend e integrações externas;
5. segurança, CORS, OpenAPI, cache, banco e infraestrutura transversal centralizados;
6. persistência compartilhada com ownership de tabelas e schemas por módulo;
7. possibilidade de rollback enquanto os deployments antigos ainda existirem.

## Estado implementado

| Capacidade | Estado |
| --- | --- |
| Processo Spring Boot único | implementado em `cloudport-runtime` |
| Autenticação | incorporada |
| Carga Geral | incorporada |
| Gate | incorporado |
| Rail | incorporado |
| Visibilidade | incorporada |
| Yard e Inventário | incorporados |
| Navio | incorporado |
| Navio Siderúrgico | incorporado |
| Navio Siderúrgico → Navio | chamada local por `CadastroNavioPorta` |
| Navio/Navio Siderúrgico → Yard | portas locais para posições, reservas, ordens, work queues, otimização e aplicação de plano |
| Gate → Autenticação | consulta local de usuário |
| Gate → Yard | consulta local de disponibilidade e status |
| TOS | adaptador HTTP externo |
| OCR, EDI e eventos externos | mensageria/adaptadores de borda |
| PostgreSQL | uma conexão, oito schemas |
| Flyway | um histórico independente por módulo |
| Segurança e CORS | uma configuração do runtime |
| OpenAPI | documento consolidado |
| Cache | gerenciador composto para os módulos que utilizam Caffeine e Redis |
| Teste de contexto | PostgreSQL por Testcontainers |
| Imagem e Compose | implementados para o runtime geral |
| EasyPanel | Dockerfiles específicos para contextos `/backend` e `/frontend` |
| Retirada dos deployments antigos | pendente de corte operacional e rollback validado |

## Visão de execução

```mermaid
flowchart TB
    CLIENTES[Portal, Control Room e clientes externos] --> RUNTIME[cloudport-runtime]

    subgraph RUNTIME[Monólito modular]
        AUTH[Autenticação]
        CGO[Carga Geral]
        GATE[Gate]
        RAIL[Rail]
        VIS[Visibilidade]
        YARD[Yard e Inventário]
        NAVIO[Navio]
        SIDERURGICO[Navio Siderúrgico]

        SIDERURGICO --> NAVIO
        SIDERURGICO --> YARD
        GATE --> AUTH
        GATE --> YARD
        VIS --> GATE
        VIS --> RAIL
        VIS --> YARD
        VIS --> NAVIO
    end

    RUNTIME --> POSTGRES[(PostgreSQL)]
    RUNTIME --> RABBIT[(RabbitMQ)]
    RUNTIME --> REDIS[(Redis)]
    RUNTIME --> EXTERNOS[TOS, OCR, EDI, storage e sistemas externos]
```

As setas internas representam chamadas locais ou eventos internos. HTTP e mensageria permanecem na borda quando a integração atravessa o processo.

## Limites dos módulos

Cada módulo deve:

- possuir pacote raiz próprio;
- expor operações internas por interfaces ou serviços de aplicação pequenos;
- não acessar controller, repository ou entidade JPA de outro módulo;
- não consultar diretamente o schema de outro módulo para substituir um contrato interno;
- não introduzir dependência cíclica;
- possuir e versionar suas próprias migrações;
- publicar evento interno quando a dependência síncrona não for necessária.

### Responsabilidades

| Módulo | Responsabilidade principal |
| --- | --- |
| Autenticação | login, JWT, usuários, papéis, permissões e navegação |
| Carga Geral | conhecimentos, itens, cargo lots, referências, estoque e movimentações de carga geral/break-bulk |
| Gate | facilities, Gates, pistas, agendamentos, truck visits, transações, documentos, EIR, inspeções e integrações |
| Rail | visitas ferroviárias, composições, ordens, vagões e operações ferroviárias |
| Visibilidade | dashboards, histórico, alertas, rastreamento e projeções de leitura |
| Yard | mapa, inventário, posições, reservas, allocations, ordens, work queues, work instructions, CHEs e reefers |
| Navio | cadastro canônico, escalas, Bay Plan, planejamento e estiva de contêineres |
| Navio Siderúrgico | visitas, itens, reservas, integração com Yard e regras de carga siderúrgica |
| Integrações | TOS, OCR, EDI, webhooks, storage e mensageria externa |

## Capacidades operacionais consolidadas

A arquitetura já suporta, dentro do mesmo runtime:

- Vessel Planner com profile, top, section e tier views sincronizadas;
- Yard georreferenciado com vistas operacionais, heatmaps, restrições, notas, movimentação e allocations;
- inventário canônico de unidades e equipamentos;
- Gate configurável e monitor visual de pistas, filas, jornada, SLA, documentos e EIR;
- composição ferroviária visual e planejamento por vagão;
- carga geral e break-bulk com domínio próprio;
- Control Room com work queues, telemetria, dispositivos, comandos e alarmes;
- Billing/CAP e central global de alertas;
- eventos internos idempotentes, SSE e contratos externos versionados.

## Comunicação

### Permitido internamente

- chamada direta por porta/interface ou serviço público do módulo proprietário;
- DTO interno estável, sem expor entidade JPA;
- evento interno no mesmo processo;
- transação coordenada somente quando a operação for realmente atômica.

### Permitido na borda

- HTTP para TOS e outros sistemas externos;
- RabbitMQ para OCR, EDI, interoperabilidade e eventos externos;
- Redis para cache e projeções;
- storage local, objeto ou serviço externo por adaptador.

### Transitório para rollback

- clientes HTTP legados condicionados por propriedade;
- `X-CloudPort-Service-Key` somente quando a chamada atravessar deployments antigos;
- imagens e configurações dos runtimes anteriores enquanto o rollback não tiver sido encerrado.

### Não permitido para código novo

- cliente HTTP entre módulos executados no `cloudport-runtime`;
- compartilhamento de repository JPA;
- acesso direto à entidade interna de outro módulo;
- novo executável Spring Boot para funcionalidade interna sem nova decisão arquitetural;
- duplicação de segurança, CORS ou OpenAPI no runtime geral.

## Persistência e Flyway

O runtime usa uma conexão PostgreSQL e preserva ownership por schema:

| Schema | Módulo proprietário |
| --- | --- |
| `cloudport_autenticacao` | Autenticação |
| `cloudport_carga_geral` | Carga Geral |
| `cloudport_gate` | Gate |
| `cloudport_rail` | Rail |
| `cloudport_visibilidade` | Visibilidade |
| `cloudport_yard` | Yard e Inventário |
| `cloudport_navio` | Navio |
| `cloudport_siderurgico` | Navio Siderúrgico |

O runtime cria oito objetos Flyway independentes antes do `EntityManagerFactory`. Cada histórico utiliza somente as migrações do artefato proprietário.

Regras:

1. uma versão Flyway não pode ser reutilizada dentro do mesmo módulo;
2. migrações aplicadas não devem ser alteradas;
3. mudanças de compatibilidade usam `expand and contract`;
4. remoções destrutivas não podem ocorrer na mesma entrega que retira o deployment antigo;
5. joins entre schemas não substituem contratos de módulo;
6. nomes de schema são validados antes do uso.

## Segurança

O runtime geral:

- expõe uma única `SecurityFilterChain`;
- valida JWT e converte claims de papéis para autoridades Spring;
- mantém a aplicação stateless;
- centraliza CORS;
- libera somente autenticação, health, documentação e assets públicos necessários;
- mantém a credencial interna apenas para compatibilidade com deployments legados;
- exige segredo JWT com no mínimo 32 bytes;
- publica um único OpenAPI consolidado.

Os executáveis standalone preservados para rollback devem falhar fechados e manter paridade de autenticação e autorização com o runtime canônico.

## Cache, mensageria e integrações

RabbitMQ permanece externo porque representa integração temporal, OCR, EDI e eventos publicados. Incorporar o módulo não transforma automaticamente esses contratos externos em chamadas diretas.

Durante o corte, somente uma instância pode consumir cada fila e executar cada job. Deployments antigos devem iniciar com consumidores, jobs e escrita desativados.

## Build

O runtime geral é construído pelo reator:

```text
backend/cloudport-modules
├── cloudport-contracts
├── servico-autenticacao
├── servico-carga-geral
├── servico-gate
├── servico-rail
├── servico-visibilidade
├── servico-yard
├── servico-navio
├── servico-navio-siderurgico
└── cloudport-runtime
```

Comandos:

```bash
cd backend/cloudport-modules
mvn -B -N -f ../cloudport-navio-modules/pom.xml -DskipTests install
mvn -B -Dspring-boot.repackage.skip=true \
  -pl :cloudport-runtime -am \
  -DskipTests install
mvn -B -pl :cloudport-runtime test package
```

Os Dockerfiles suportados são:

- `backend/Dockerfile`, para build com contexto `/backend` no EasyPanel;
- `backend/cloudport-runtime/Dockerfile`, para build a partir da raiz;
- `frontend/Dockerfile`, para o portal React com Nginx.

## Implantação

O Compose em `deploy/cloudport-runtime/docker-compose.yml` inicia PostgreSQL, RabbitMQ, Redis e `cloudport-runtime`.

O runtime geral é o único escritor e executor de jobs no perfil consolidado. O proxy e o frontend devem usar uma única origem de API.

### Critérios para retirar os deployments antigos

1. paridade dos endpoints usados;
2. autenticação e autorização validadas;
3. migrações e dados compatíveis;
4. somente uma execução de jobs e consumidores;
5. frontend e integrações externas testados;
6. health, logs, métricas e alertas disponíveis;
7. testes unitários, integração, contrato e e2e aprovados;
8. proxy apontando para o runtime geral;
9. rollback testado;
10. branch sincronizada e sem conflitos.

## Rollback

Enquanto os deployments antigos existirem, o rollback deve:

1. interromper o runtime geral antes de reativar escrita, jobs ou consumidores antigos;
2. preservar schemas e históricos Flyway compatíveis;
3. redirecionar a origem de API para o deployment anterior;
4. reativar variáveis e adaptadores legados documentados;
5. impedir escrita concorrente entre os dois modelos;
6. não tentar desfazer migração aditiva já aplicada.

O `cloudport-monolito-navio` pode ser utilizado como rollback intermediário do domínio Navio. A retirada definitiva dele e dos serviços antigos ocorrerá somente após a estabilização do runtime geral.

## Pendências arquiteturais

- executar e testar o corte operacional do runtime geral em ambiente;
- validar o rollback completo com os oito schemas já migrados;
- concluir a centralização das políticas transversais que ainda estejam duplicadas em executáveis standalone;
- eliminar clientes e credenciais HTTP legados após encerrar a janela de retorno;
- manter segurança fail-closed nos canais e executáveis preservados para rollback;
- renomear artefatos `servico-*` somente após estabilização dos pipelines.
