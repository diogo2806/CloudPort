# Arquitetura do monólito modular CloudPort

## Status da decisão

- Estado: vigente.
- Arquitetura alvo: monólito modular.
- Runtime geral: `backend/cloudport-runtime`.
- Primeiro corte preservado para rollback: `backend/cloudport-monolito-navio`.
- Módulos incorporados: Autenticação, Gate, Rail, Visibilidade, Yard, Navio e Navio Siderúrgico.

Este documento é a referência principal para estrutura, comunicação, persistência, segurança, build, implantação e rollback do backend.

## Decisão

O CloudPort executa suas funcionalidades internas em um único processo Spring Boot, mantendo limites explícitos entre os módulos de negócio.

O runtime geral deve possuir:

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
| Processo Spring Boot único | Implementado em `cloudport-runtime` |
| Autenticação | Incorporada; emissão e validação continuam separadas por componentes internos |
| Gate | Incorporado |
| Rail | Incorporado |
| Visibilidade | Incorporada |
| Yard | Incorporado |
| Navio | Incorporado |
| Navio Siderúrgico | Incorporado |
| Navio Siderúrgico -> Navio | Chamada local por `CadastroNavioPorta` |
| Navio -> Yard | Chamadas locais para ordens, work queues e posições reserváveis |
| Gate -> Autenticação | Consulta local de usuário |
| Gate -> status do Yard | Consulta local de disponibilidade |
| TOS | Adaptador HTTP externo |
| OCR | Adaptador RabbitMQ externo |
| EDI | Adaptadores externos e mensageria |
| RabbitMQ e Redis | Infraestrutura externa |
| PostgreSQL | Uma conexão, sete schemas |
| Flyway | Um histórico independente por módulo |
| Segurança e CORS | Uma configuração do runtime |
| OpenAPI | Um documento consolidado |
| Cache | Gerenciador composto para Gate e Visibilidade |
| Teste de contexto | PostgreSQL 16 por Testcontainers |
| Imagem e Compose | Implementados para o runtime geral |
| Retirada dos deployments antigos | Pendente de corte operacional e rollback validado |

## Visão de execução

```mermaid
flowchart TB
    CLIENTES[Portal, Control Room e clientes externos] --> RUNTIME[cloudport-runtime]

    subgraph RUNTIME[Monólito modular]
        AUTH[Autenticação]
        GATE[Gate]
        RAIL[Rail]
        VIS[Visibilidade]
        YARD[Yard]
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
- não consultar diretamente o schema de outro módulo para evitar um contrato interno;
- não introduzir dependência cíclica;
- possuir e versionar suas próprias migrações;
- publicar evento interno quando a dependência síncrona não for necessária.

### Responsabilidades

| Módulo | Responsabilidade principal |
| --- | --- |
| Autenticação | login, emissão de JWT, usuários, papéis e permissões |
| Gate | agendamentos, visitas de caminhão, transações, documentos e integrações de gate |
| Rail | visitas ferroviárias, composições, ordens e operações ferroviárias |
| Visibilidade | dashboards, histórico, alertas e projeções de leitura |
| Yard | mapa, posições, reservas, ordens, work queues e work instructions |
| Navio | cadastro canônico, escalas e estiva |
| Navio Siderúrgico | visitas operacionais, itens, reservas, integração com Yard e regras siderúrgicas |
| Integrações | TOS, OCR, EDI, webhooks, storage e mensageria externa |

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

- cliente HTTP entre módulos que executam no `cloudport-runtime`;
- compartilhamento de repository JPA;
- acesso direto à entidade interna de outro módulo;
- novo executável Spring Boot para funcionalidade interna sem nova decisão arquitetural;
- duplicação de segurança, CORS ou OpenAPI no runtime geral.

## Persistência e Flyway

O runtime usa uma conexão PostgreSQL e preserva ownership por schema:

| Schema | Módulo proprietário |
| --- | --- |
| `cloudport_autenticacao` | Autenticação |
| `cloudport_gate` | Gate |
| `cloudport_rail` | Rail |
| `cloudport_visibilidade` | Visibilidade |
| `cloudport_yard` | Yard |
| `cloudport_navio` | Navio |
| `cloudport_siderurgico` | Navio Siderúrgico |

O runtime cria sete objetos Flyway independentes antes do `EntityManagerFactory`. Cada histórico utiliza somente as migrações do artefato proprietário.

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
- valida JWT HS256 e converte claims de papéis para autoridades Spring;
- mantém a aplicação stateless;
- centraliza CORS;
- libera somente autenticação, health, documentação e assets públicos necessários;
- mantém o filtro de credencial interna apenas para compatibilidade com deployments legados;
- exige segredo JWT com no mínimo 32 bytes;
- publica um único OpenAPI consolidado.

## Cache, mensageria e integrações

O cache do runtime combina:

- Caffeine para contratos TOS do Gate;
- Redis para projeções da Visibilidade.

RabbitMQ permanece externo porque representa integração temporal, OCR, EDI e eventos publicados. Incorporar o módulo não transforma automaticamente esses contratos externos em chamadas diretas.

Durante o corte, somente uma instância pode consumir cada fila e executar cada job. Deployments antigos devem iniciar com consumidores, jobs e escrita desativados.

## Build

O runtime geral é construído pelo reator:

```text
backend/cloudport-modules
├── servico-autenticacao
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
mvn -B -Dspring-boot.repackage.skip=true \
  -pl :cloudport-runtime -am \
  -DskipTests install
mvn -B -pl :cloudport-runtime test package
```

O `Dockerfile` da raiz do runtime gera um único JAR executável e executa como usuário não privilegiado.

## Implantação

O Compose em `deploy/cloudport-runtime/docker-compose.yml` inicia:

- PostgreSQL;
- RabbitMQ;
- Redis;
- `cloudport-runtime`.

O runtime geral é o único escritor e executor de jobs no perfil consolidado. O proxy ou frontend deve usar uma única origem de API.

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

- executar e testar o corte operacional do runtime geral;
- criar smoke completo incluindo RabbitMQ, Redis, Gate, Rail e Visibilidade;
- centralizar tratamento de erros, logs, métricas e tracing ainda definidos localmente;
- substituir eventos periódicos internos por eventos no processo quando aplicável;
- remover clientes e credenciais HTTP legados após encerrar o rollback;
- consolidar versões e plugins em parent Maven compartilhado;
- renomear artefatos `servico-*` somente após estabilização dos pipelines.
