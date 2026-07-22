# CloudPort

CloudPort é uma plataforma portuária para operação de contêineres, carga geral, break-bulk, carga siderúrgica, Gate, ferrovia, pátio, navios, faturamento e visibilidade operacional.

O backend oficial é o monólito modular `backend/cloudport-runtime`. Os módulos mantêm ownership próprio de dados, migrações e regras de negócio, mas executam em um único processo Spring Boot. TOS, OCR, EDI, RabbitMQ, Redis, storage e demais sistemas permanecem integrações de borda.

## Capacidades implementadas

| Domínio | Capacidades atuais |
| --- | --- |
| Autenticação | Login JWT, usuários, papéis, permissões, configuração dinâmica da navegação, usuário root configurável por ambiente e encerramento seguro da sessão no portal. |
| Gate | Facilities, múltiplos Gates e pistas, estágios, business tasks, appointments, truck visits, bookings, Bill of Lading, EDO, ERO, IDO, pré-avisos, inspeções, trouble transactions, documentos, imagens, tickets, EIR, regras de acesso, transferências e controle de pessoas. |
| Gate visual | Quadro de pistas e filas, calendário de capacidade, jornada do veículo, OCR, balança, inspeção, liberação, SLA e transações problemáticas. |
| Pátio | Google Maps, blocos e pilhas, vistas de bloco, seção, scan e microvisão, workspaces, movimentação gráfica, restrições, notas, heatmaps, rotas, CHEs, work queues, work instructions, allocations e reshuffling. |
| Otimização | Agrupamento de recebimento e atribuição global de posições por custo mínimo, considerando restrições físicas, reefer, perigosos, reservas, distância e risco de rehandle. |
| Inventário | Unidades e equipamentos, contêineres, chassis, carretas, acessórios, tipos, prefixos, lacres, documentos, avarias, manutenção, holds, ownership, vínculos, reefer, inventário físico e divergências. |
| Ferrovia | Visitas, manifestos, vagões, ordens, lista de trabalho, conclusão e partida, line-up vertical, composição gráfica, ocupação de linhas e transferência de locomotiva para navio. |
| Navio | Cadastro canônico, visitas, line-up interno e público, modelos geométricos versionados, BAPLIE, Vessel Planner, estabilidade, força estrutural, segregação, restow e sequência de guindastes. |
| Vessel Planner visual | Profile, top, section e tier sincronizados, drag-and-drop, inspector de slot, legendas, tampas de porão, peso por stack, restrições, IMDG e overlays técnicos. |
| Carga siderúrgica | Planejamento de bobinas, tank top, empilhamento, dunnage, calçamento, securing, estabilidade e aprovação baseada em evidências versionadas. |
| Carga geral e break-bulk | Bill of Lading, itens, cargo lots, commodities, embalagens, produtos, armazenagem, manuseio, perigosos, temperatura, avarias, movimentação parcial, consolidação e desconsolidação. |
| Control Room | Equipamentos, telemetria, conectividade, VMT, work instruction atual, alarmes, indisponibilidades, comandos remotos e dispositivos. |
| Visibilidade | Dashboards, rastreamento, histórico, alertas, central global, reconhecimento e resolução, correlação e eventos versionados. |
| Billing e CAP | Tarifas, cobranças, faturas, itens, pagamentos, isolamento por transportadora e portal de acompanhamento. |
| Interface compartilhada | `OperationalDataGrid`, filtros, ordenação, paginação, colunas configuráveis, seleção múltipla, inspector, CSV, Excel, visões salvas e ajuda contextual. |

## Fluxos especiais

- saída direta de carga autopropelida descarregada do navio pelo Gate;
- embarque de contêiner diretamente do Gate para o navio, sem permanência no pátio;
- locomotiva isolada tratada como a própria visita ferroviária e embarcada após custódia, planejamento e checklist;
- line-up público de navios em `/line-up`;
- planejamento de recebimento de contêineres antes da reserva de posições.

## Arquitetura

O runtime incorpora oito módulos de negócio:

1. Autenticação;
2. Carga Geral;
3. Gate;
4. Rail;
5. Visibilidade;
6. Yard;
7. Navio;
8. Navio Siderúrgico.

`backend/cloudport-contracts` concentra contratos compartilhados. O PostgreSQL usa uma conexão e oito schemas:

- `cloudport_autenticacao`;
- `cloudport_carga_geral`;
- `cloudport_gate`;
- `cloudport_rail`;
- `cloudport_visibilidade`;
- `cloudport_yard`;
- `cloudport_navio`;
- `cloudport_siderurgico`.

Cada módulo mantém histórico Flyway independente.

## Estrutura principal

```text
backend/
├── cloudport-navio-modules/       # parent Maven compartilhado
├── cloudport-modules/             # reator Maven canônico
├── cloudport-contracts/
├── cloudport-runtime/
├── cloudport-monolito-navio/
├── servico-autenticacao/
├── servico-carga-geral/
├── servico-gate/
├── servico-rail/
├── servico-visibilidade/
├── servico-yard/
├── servico-navio/
└── servico-navio-siderurgico/
frontend/
├── cloudport/
├── Dockerfile
└── nginx.conf
deploy/
├── cloudport-runtime/
└── navio-monolito/
```

## Build do backend

Pré-requisitos: JDK 17, Maven 3.9+ e Docker.

O parent compartilhado em `backend/cloudport-navio-modules` deve ser instalado antes do reator `backend/cloudport-modules`, que agrega contratos, módulos de domínio e o runtime executável.

```bash
cd backend/cloudport-modules
mvn -B -N -f ../cloudport-navio-modules/pom.xml -DskipTests install
mvn -B -Dspring-boot.repackage.skip=true \
  -pl :cloudport-runtime -am \
  -DskipTests install
mvn -B -pl :cloudport-runtime test package
```

Execução direta:

```bash
java -jar backend/cloudport-runtime/target/cloudport-runtime-*.jar
```

## Build do frontend

```bash
cd frontend/cloudport
npm install
npm run build
```

## Variáveis obrigatórias do backend

```bash
export DB_HOST='localhost'
export DB_PORT='5432'
export DB_NAME='cloudport'
export DB_USER='cloudport'
export DB_PASS='substitua-a-senha-do-postgres'
export DB_SCHEMA='cloudport_autenticacao,cloudport_carga_geral,cloudport_gate,cloudport_rail,cloudport_visibilidade,cloudport_yard,cloudport_navio,cloudport_siderurgico'
export SECURITY_JWT_SECRET='substitua-por-segredo-com-pelo-menos-32-bytes'
export SECURITY_JWT_EXPIRATION_MS='7200000'
export ADMIN_EMAIL='root@cloudport.local'
export ADMIN_PASSWORD='substitua-por-uma-senha-segura'
```

Regras:

- `SECURITY_JWT_SECRET` deve possuir pelo menos 32 bytes;
- `ADMIN_EMAIL` e `ADMIN_PASSWORD` definem as credenciais do usuário root;
- o root recebe `ROLE_ROOT` e todos os papéis cadastrados, inclusive papéis adicionados posteriormente;
- a senha do root é sincronizada com `ADMIN_PASSWORD` em cada inicialização;
- o sistema não mantém credenciais root funcionais padrão inseguras;
- `DB_SCHEMA` deve conter os oito schemas do runtime;
- não versionar valores reais de senha ou segredo.

## Docker Compose

Além das variáveis obrigatórias, configure as integrações externas:

```bash
export CLOUDPORT_INTERNAL_SERVICE_KEY='substitua-a-chave-interna'
export SECURITY_CORS_ALLOWED_ORIGINS='http://localhost:4200,http://localhost:8080'
export TOS_API_BASE_URL='http://localhost:8090'
```

A mensageria fica desabilitada por padrão. Nesse modo, o container RabbitMQ não é criado:

```bash
export RABBITMQ_ENABLED=false

docker compose \
  -f deploy/cloudport-runtime/docker-compose.yml \
  up -d --build
```

Para usar o RabbitMQ fornecido pelo próprio Compose, habilite o profile `messaging`:

```bash
export RABBITMQ_ENABLED=true
export RABBITMQ_PASSWORD='substitua-a-senha-do-rabbitmq'

docker compose \
  -f deploy/cloudport-runtime/docker-compose.yml \
  --profile messaging \
  up -d --build
```

Para um RabbitMQ externo, mantenha o profile desligado e configure `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME` e `RABBITMQ_PASSWORD`. Quando `RABBITMQ_ENABLED=true` e o host/porta não estão acessíveis, o runtime encerra com código `78` e informa que o profile deve ser habilitado ou a conexão externa corrigida.

A API é publicada por padrão na porta `8080`.

## EasyPanel

### Backend

| Campo | Valor |
| --- | --- |
| Caminho de build | `/backend` |
| Dockerfile | `Dockerfile` |
| Porta | `8080` |
| Health check | `/actuator/health/readiness` |

O arquivo correto para esse contexto é `backend/Dockerfile`. `backend/cloudport-runtime/Dockerfile` é usado quando o build parte da raiz.

### Frontend

| Campo | Valor |
| --- | --- |
| Caminho de build | `/frontend` |
| Dockerfile | `Dockerfile` |
| Porta | `80` |
| Health check | `/health` |

A imagem usa Node.js no build e Nginx para servir a SPA com fallback para `index.html`.

## Segurança do corte

Durante a operação consolidada, somente uma instância pode aceitar escritas, executar jobs e consumir cada fila. Deployments anteriores devem permanecer parados ou com estas capacidades desativadas:

```text
CLOUDPORT_WRITES_ENABLED=false
CLOUDPORT_JOBS_ENABLED=false
CLOUDPORT_CONSUMERS_ENABLED=false
```

O rollback troca o binário e o roteamento, sem downgrade do banco. O procedimento completo está em `docs/operacao-corte-rollback-navio.md`.

## Documentação

- [Arquitetura do monólito modular](docs/arquitetura-monolito-modular.md)
- [Operação de corte e rollback](docs/operacao-corte-rollback-navio.md)
- [Requisitos implementados](docs/implementados/requisitos-implementados.md)
- [Backlog funcional e de integração](docs/requisitos/modulo-navios-back-front-gaps.md)
- [Pendências técnicas comprovadas](docs/requisitos/requisito-tecnico.md)
- [Runtime canônico](backend/cloudport-runtime/README.md)

## Observações

O replanejamento visual de vagões ainda precisa ser persistido por um contrato operacional. O overlay visual de lashing auxilia a análise, mas não substitui cálculo certificado.
