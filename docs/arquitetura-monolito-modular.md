# Arquitetura do monólito modular CloudPort

## Status da decisão

- Estado: vigente.
- Arquitetura alvo: monólito modular.
- Runtime: `backend/cloudport-monolito-navio`, mantido com o nome histórico durante a transição.
- Módulos incorporados no código: Navio, Navio Siderúrgico, Yard, Gate, Rail, Autenticação e Visibilidade.
- Estado operacional: o corte de ambiente e a retirada dos deployments legados continuam condicionados à validação de paridade e rollback.

Este documento é a referência principal para estrutura, comunicação, dados, segurança, build e implantação do backend.

## Decisão

O CloudPort evolui como um único produto implantável, dividido internamente em módulos de domínio. Cada módulo conserva responsabilidade, pacotes, contratos e propriedade de dados, mas os módulos incorporados executam no mesmo processo Spring Boot.

O runtime consolidado deve garantir:

1. um único artefato e processo de aplicação;
2. dependências direcionais e sem ciclos;
3. comunicação interna por portas ou eventos;
4. HTTP e mensageria somente na borda ou durante rollback;
5. segurança, CORS, Jackson, erros, logs, métricas, tracing e agendamento centralizados;
6. uma conexão PostgreSQL, com ownership explícito por schema;
7. históricos Flyway independentes e compatíveis com retorno de aplicação;
8. apenas um escritor, executor de jobs e grupo ativo de consumidores durante o corte.

## Runtime e módulos

```text
cloudport-navio-modules
├── servico-navio
├── servico-navio-siderurgico
├── servico-yard
├── servico-gate
├── servico-rail
├── servico-autenticacao
├── servico-visibilidade
└── cloudport-monolito-navio
```

Os diretórios `servico-*` continuam existindo como limites de módulo e para preservar compilação isolada e rollback. O nome não implica deployment independente no estado alvo.

| Módulo | Responsabilidade principal |
| --- | --- |
| Navio | cadastro canônico, escalas, visitas, planos de estiva e eventos marítimos |
| Navio Siderúrgico | operações, itens, reservas e regras específicas de carga siderúrgica |
| Yard | mapa, posições, reservas, movimentos, ordens, work queues e work instructions |
| Gate | agendamentos, visitas de caminhão, transações, documentos, OCR e TOS externo |
| Rail | visitas ferroviárias, composição, ordens e movimentos ferroviários |
| Autenticação | usuários, papéis, políticas, login e emissão de token |
| Visibilidade | projeções, dashboards, alertas, histórico e streaming operacional |

## Limites e dependências

Cada módulo deve:

- possuir pacote raiz próprio;
- expor operações internas por interfaces pequenas;
- receber e retornar DTOs de contrato, nunca entidades JPA de outro módulo;
- acessar somente seus próprios repositories;
- publicar eventos internos quando não houver necessidade de resposta síncrona;
- manter suas migrações dentro do próprio artefato;
- não depender do pacote do runtime.

Não é permitido:

- acessar controller, service interno, entidade ou repository de outro módulo para contornar um contrato;
- criar cliente HTTP entre módulos incorporados;
- criar dependência cíclica;
- duplicar cadeia de segurança, CORS, tratamento global de erros ou agendamento dentro do runtime;
- criar novo executável Spring Boot para funcionalidade interna sem nova decisão arquitetural.

Os testes ArchUnit do runtime verificam ausência de ciclos, dependência de módulo para o runtime, uso de adaptadores HTTP pelo runtime e acesso direto a repositories de outro módulo.

## Comunicação interna

### Portas locais implementadas

| Origem | Destino | Porta | Implementação local |
| --- | --- | --- | --- |
| Navio Siderúrgico | Navio | `CadastroNavioPorta` | `CadastroNavioLocalAdapter` |
| Navio Siderúrgico | Yard | `OrdemPatioYardCliente` | `OrdemPatioLocalAdapter` |
| Navio Siderúrgico | Yard | `PosicaoPatioYardCliente` | `PosicaoPatioLocalAdapter` |
| Gate | Yard | `ClienteStatusPatio` | `StatusPatioLocalAdapter` |
| Gate | Autenticação | `AutenticacaoClient` | `AutenticacaoLocalAdapter` |

Os nomes históricos terminados em `Cliente` foram mantidos para reduzir impacto de migração, mas agora representam portas. Os adaptadores `*HttpAdapter` são registrados somente quando a propriedade de integração está em `http`. No runtime consolidado, as propriedades ficam em `local`.

### Eventos

Eventos Spring internos podem substituir chamadas síncronas quando o consumidor não precisa responder à mesma transação. RabbitMQ permanece válido para integrações externas, desacoplamento temporal real e coexistência com deployments ainda não cortados.

Consumidores externos devem ser idempotentes. Eventos públicos devem possuir versão, `eventId`, `occurredAt`, `correlationId`, origem e chave de agregação. Eventos internos não devem ser publicados externamente sem adaptação explícita.

## Ownership de schemas e tabelas

O runtime usa uma conexão PostgreSQL e um schema por módulo:

| Schema | Proprietário | Local das migrações |
| --- | --- | --- |
| `cloudport_navio` | Navio | `classpath:cloudport/migrations/navio` |
| `cloudport_siderurgico` | Navio Siderúrgico | `classpath:cloudport/migrations/navio-siderurgico` |
| `cloudport_yard` | Yard | `classpath:cloudport/migrations/yard` |
| `cloudport_gate` | Gate | `classpath:cloudport/migrations/gate` |
| `cloudport_rail` | Rail | `classpath:cloudport/migrations/rail` |
| `cloudport_autenticacao` | Autenticação | `classpath:cloudport/migrations/autenticacao` |
| `cloudport_visibilidade` | Visibilidade | `classpath:cloudport/migrations/visibilidade` |

A regra de ownership é objetiva: toda tabela, índice, sequence, constraint, view ou função criada por uma migração localizada no artefato de um módulo pertence àquele módulo. Somente o módulo proprietário pode alterar sua estrutura.

Regras adicionais:

1. repositories permanecem no módulo proprietário;
2. joins entre schemas não substituem portas;
3. uma foreign key entre schemas exige decisão registrada e não transfere ownership;
4. projeções de Visibilidade armazenam cópias de leitura e não se tornam fonte de verdade;
5. renomear ou mover tabela exige migração do proprietário, período de compatibilidade e plano de retorno;
6. nomes de schema são validados antes de configurar o Flyway.

## Flyway e compatibilidade

O runtime cria um objeto Flyway por módulo, todos antes do `EntityManagerFactory`. Cada schema conserva sua própria tabela `flyway_schema_history`.

Durante a janela de rollback:

- não editar migração já aplicada;
- corrigir por nova versão;
- não renumerar históricos existentes;
- executar `validate` em todos os schemas antes do corte;
- usar `expand and contract`;
- adicionar estruturas antes de remover ou renomear;
- manter campos antigos enquanto o binário anterior puder voltar;
- fazer backfill idempotente;
- adiar `DROP`, redução de tamanho, tipo incompatível e `NOT NULL` sem valor padrão;
- tratar rollback normal como troca de aplicação e roteamento, nunca como downgrade automático do banco.

Mudança destrutiva só pode ocorrer depois do encerramento formal da janela de rollback e da retirada da versão anterior.

## Execução única

O corte usa três controles independentes:

| Controle | Propriedade | Runtime ativo | Legado em coexistência |
| --- | --- | --- | --- |
| Comandos de escrita | `cloudport.runtime.writes-enabled` | `true` | `false` |
| Jobs | `cloudport.runtime.jobs-enabled` | `true` | `false` |
| Consumidores RabbitMQ | `cloudport.runtime.consumers-enabled` | `true` | `false` |

Com escrita desativada, `POST`, `PUT`, `PATCH` e `DELETE` retornam `503`. Com jobs desativados, o agendamento não é habilitado. Com consumidores desativados, os containers RabbitMQ não iniciam.

Jobs críticos também usam `pg_try_advisory_xact_lock`. O bloqueio distribuído é a proteção contra duas instâncias do mesmo runtime compartilhando PostgreSQL, mas não substitui a configuração correta de escritor, jobs e consumidores.

Comandos e consumidores devem possuir chave de idempotência ou estado persistente quando puderem ser repetidos por retry, redelivery ou falha após commit.

## Configuração transversal central

O runtime é responsável por:

- parent Maven, versões, `dependencyManagement`, `pluginManagement` e requisitos de Java/Maven;
- uma cadeia de segurança stateless;
- emissão e validação de token por componentes internos do módulo Autenticação;
- conversão de `roles` e `role` para autoridades Spring Security;
- uma política CORS;
- um `ObjectMapper` com Java Time, UTC e contrato consistente;
- tratamento global de erros com `codigo`, `mensagem`, `detalhes`, `correlationId`, caminho, status e timestamp;
- propagação de `X-Correlation-Id` e `traceId` no MDC;
- métricas HTTP e exportação Prometheus;
- documentação OpenAPI consolidada;
- scheduler central condicionado por propriedade;
- cliente HTTP comum para integrações realmente externas;
- conversor JSON principal do RabbitMQ.

Configurações isoladas continuam nos artefatos apenas para execução standalone e são excluídas do component scan do runtime.

## Segurança

O runtime:

- mantém sessão stateless;
- exige segredo HS256 com pelo menos 32 bytes;
- libera somente login, health, documentação e assets públicos necessários;
- protege as demais rotas por JWT e autorização de método;
- aplica CORS em um único ponto;
- conserva `X-CloudPort-Service-Key` apenas para comunicação com deployments legados durante a janela de rollback.

Chamadas locais não usam credencial de serviço. A credencial e seus segredos só podem ser removidos depois que nenhum tráfego operacional depender dos adaptadores HTTP legados.

## Build e empacotamento

Os módulos produzem JARs de biblioteca no perfil `modulo-monolito`. O runtime depende desses artefatos e gera o único JAR executável.

```bash
cd backend/cloudport-navio-modules
mvn -B -Pmodulo-monolito -pl :cloudport-monolito-navio -am test package
```

As migrações e demais recursos são publicados dentro do artefato proprietário. O runtime não adiciona diretórios de fontes de irmãos por plugin Maven.

A imagem Docker compila os sete módulos, incorpora o frontend do Control Room e inicia somente o JAR consolidado.

## Implantação e rollback

O deployment legado não é removido no mesmo passo em que o módulo é incorporado. A sequência obrigatória é:

1. validar build, testes, OpenAPI e ausência de rotas duplicadas;
2. validar todos os históricos Flyway e dados essenciais;
3. criar backup e registrar o ponto de restauração;
4. iniciar o monólito com escrita, jobs e consumidores ativos;
5. manter legados sem escrita, jobs e consumidores;
6. direcionar cada rota para exatamente um backend;
7. executar smoke e comparação funcional;
8. ensaiar retorno para os binários legados sobre os mesmos schemas;
9. observar o runtime pelo período definido pela operação;
10. somente depois remover deployments, imagens, credenciais e variáveis legadas.

O rollback para o legado exige primeiro retirar e parar o monólito. Nunca podem existir dois escritores ou dois grupos ativos de consumidores para o mesmo domínio.

O procedimento detalhado está em `docs/operacao-corte-rollback-navio.md`.

## Critérios para concluir a migração

A migração só é considerada concluída quando:

- todos os módulos estão no mesmo runtime em produção;
- nenhuma chamada HTTP ocorre entre módulos incorporados;
- contratos externos mantêm paridade;
- uma única origem de API atende o frontend;
- todos os schemas e históricos Flyway estão validados;
- jobs, consumidores e comandos executam uma única vez;
- testes de arquitetura passam;
- logs, métricas, tracing e erros usam o padrão central;
- rollback foi ensaiado;
- deployments e credenciais legadas foram removidos de forma controlada.
