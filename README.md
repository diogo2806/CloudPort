# CloudPort

CloudPort é uma plataforma portuária para operação de contêineres, carga geral, break-bulk, carga siderúrgica, Gate, ferrovia, pátio, navios, faturamento e visibilidade operacional.

O backend oficial é o monólito modular `backend/cloudport-runtime`. Os módulos mantêm ownership próprio de dados, migrações e regras de negócio, mas executam em um único processo Spring Boot. Integrações externas, como TOS, OCR, EDI, RabbitMQ, Redis e storage, permanecem na borda da aplicação.

## Estado atual

| Domínio | Capacidades implementadas |
| --- | --- |
| Autenticação | Login JWT, usuários, papéis, permissões, configuração dinâmica da navegação e encerramento seguro da sessão no portal. |
| Gate | Facilities, múltiplos Gates e pistas, estágios e business tasks, appointments, truck visits, bookings, Bill of Lading, EDO, ERO, IDO, pré-avisos, inspeções, trouble transactions, documentos, imagens, tickets, EIR, regras de acesso, transferências entre instalações e controle de entrada e saída de pessoas. |
| Gate visual | Quadro de pistas e filas, calendário de capacidade, jornada do veículo, OCR, balança, inspeção, liberação, SLA, transações problemáticas e impressão/reimpressão de EIR. |
| Pátio | Mapa georreferenciado com Google Maps, blocos e pilhas, vistas de bloco, seção, scan e microvisão, workspaces, movimentação gráfica, restrições, notas, heatmaps, rotas, CHEs, work queues, work instructions, allocations, reshuffling e planejamento de recebimento. |
| Otimização do pátio | Agrupamento por compatibilidade operacional e atribuição global de posições por custo mínimo, considerando capacidade, peso, apoio físico, reefer, perigosos, reservas, distância e risco de rehandle. |
| Inventário | Domínio canônico de unidades e equipamentos, contêineres, chassis, carretas, acessórios, tipos, prefixos, lacres, documentos, avarias, manutenção, holds, ownership, vínculos, reefer, inventário físico e divergências. |
| Ferrovia | Visitas, manifestos, vagões, ordens, lista de trabalho, conclusão e partida, line-up vertical, composição gráfica, ocupação de linhas, progresso por vagão e transferência intermodal de locomotiva para navio. |
| Navio | Cadastro canônico, visitas, line-up operacional e público, modelos geométricos versionados, Vessel Planner, BAPLIE, estabilidade, força estrutural, segregação, restow e sequenciamento de guindastes. |
| Vessel Planner visual | Profile, top, section e tier sincronizados, drag-and-drop, inspector de slot, legendas operacionais, tampas de porão, peso por stack, restrições, IMDG, restow e overlays técnicos. |
| Carga siderúrgica | Planejamento de bobinas, tank top, empilhamento, dunnage, calçamento, securing, estabilidade e aprovação baseada em evidências versionadas. |
| Carga geral e break-bulk | Bill of Lading, itens, cargo lots, commodities, embalagens, produtos, armazenagem, manuseio, perigosos, temperatura, avarias, movimentação parcial, consolidação, desconsolidação e vínculos logísticos. |
| Control Room | Equipamentos, telemetria, conectividade, VMT, work instruction atual, alarmes, indisponibilidades, comandos remotos, dispositivos e atualização por SSE. |
| Visibilidade | Dashboards, histórico, alertas operacionais, central global, reconhecimento e resolução, correlação e eventos versionados. |
| Billing e CAP | Tarifas, cobranças, faturas, itens, pagamentos, isolamento por transportadora e portal de acompanhamento. |
| Interface compartilhada | `OperationalDataGrid` com busca, filtros, ordenação, paginação, colunas configuráveis, seleção múltipla, ações em lote, inspector, CSV, Excel e visões salvas; ajuda contextual em todas as páginas padrão. |

## Fluxos especiais implementados

- saída direta de carga autopropelida descarregada do navio pelo Gate;
- embarque de contêiner diretamente do Gate para o navio, sem permanência no pátio;
- locomotiva isolada tratada como a própria visita ferroviária e embarcada no navio após transferência de custódia e checklist;
- line-up público de navios em `/line-up`, com contrato sanitizado e acesso anônimo;
- planejamento de recebimento e agrupamento de contêineres antes da reserva de posições no pátio.

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

O módulo `backend/cloudport-contracts` concentra contratos compartilhados. O PostgreSQL utiliza uma conexão e oito schemas independentes:

- `cloudport_autenticacao`;
- `cloudport_carga_geral`;
- `cloudport_gate`;
- `cloudport_rail`;
- `cloudport_visibilidade`;
- `cloudport_yard`;
- `cloudport_navio`;
- `cloudport_siderurgico`.

Cada módulo mantém seu próprio histórico Flyway.

## Estrutura principal

```text
backend/
├── cloudport-contracts/
├── cloudport-modules/
├── cloudport-runtime/
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
└── cloudport-runtime/
docs/
├── arquitetura-monolito-modular.md
├── operacao-corte-rollback-navio.md
├── implementados/
└── requisitos/
```

## Build do backend

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

## Variáveis mínimas do backend

```text
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
CLOUDPORT_SECURITY_JWT_SECRET
CLOUDPORT_SECURITY_JWT_EXPIRATION
SPRING_RABBITMQ_HOST
SPRING_RABBITMQ_USERNAME
SPRING_RABBITMQ_PASSWORD
SPRING_REDIS_HOST
SPRING_REDIS_PORT
```

O segredo JWT deve possuir pelo menos 32 bytes. Credenciais funcionais padrão não são aceitas.

## Docker Compose

A partir da raiz do repositório:

```bash
docker compose \
  -f deploy/cloudport-runtime/docker-compose.yml \
  up -d --build
```

A API é publicada por padrão na porta `8080`.

## EasyPanel

### Backend

| Campo | Valor |
| --- | --- |
| Caminho de build | `/backend` |
| Dockerfile | `Dockerfile` |
| Porta | `8080` |
| Health check | `/actuator/health/readiness` |

O arquivo correto para esse contexto é `backend/Dockerfile`. O arquivo `backend/cloudport-runtime/Dockerfile` é usado quando o build parte da raiz do repositório.

### Frontend

| Campo | Valor |
| --- | --- |
| Caminho de build | `/frontend` |
| Dockerfile | `Dockerfile` |
| Porta | `80` |
| Health check | `/health` |

O container do frontend usa Node.js para o build e Nginx para servir a SPA, com fallback para `index.html`.

## Segurança operacional do corte

Durante a operação consolidada, somente uma instância pode aceitar escritas, executar jobs e consumir cada fila. Deployments anteriores devem permanecer parados ou com estas capacidades desativadas:

```text
CLOUDPORT_WRITES_ENABLED=false
CLOUDPORT_JOBS_ENABLED=false
CLOUDPORT_CONSUMERS_ENABLED=false
```

O rollback não executa downgrade de banco. O procedimento completo está em `docs/operacao-corte-rollback-navio.md`.

## Documentação

- [Arquitetura do monólito modular](docs/arquitetura-monolito-modular.md)
- [Operação de corte e rollback](docs/operacao-corte-rollback-navio.md)
- [Requisitos implementados](docs/implementados/requisitos-implementados.md)
- [Pendências técnicas atuais](docs/requisitos/requisito-tecnico.md)
- [Lacunas funcionais remanescentes](docs/requisitos/modulo-navios-back-front-gaps.md)

## Observações

O replanejamento visual de vagões ainda precisa ser persistido por um contrato operacional específico. O overlay visual de lashing auxilia a análise, mas não substitui cálculo certificado. As pendências técnicas comprovadas permanecem no arquivo `docs/requisitos/requisito-tecnico.md`.