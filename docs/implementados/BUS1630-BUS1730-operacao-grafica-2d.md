# BUS1630 a BUS1730 — operação gráfica 2D

## Escopo entregue

A interface gráfica operacional 2D foi ampliada no Vessel Planner e nas vistas do pátio para concluir os requisitos BUS1630 a BUS1730.

## BUS1630 — Flow tools

- seleção de várias unidades e destinos;
- padrões Stack-wise e Tier-wise;
- sentido crescente e decrescente;
- Paired 20 com validação de comprimento;
- alternância de bays;
- preview numerado;
- detecção de destinos insuficientes ou duplicados;
- confirmação por comando único e idempotente.

## BUS1640 — Work queues no perfil do navio

- agrupamento por fila, bay e guindaste;
- planejado e restante;
- estado e motivo de bloqueio;
- lista de movimentos;
- produtividade e término previsto.

## BUS1650 — Quay Commander

- filas arrastáveis;
- divisão por ponto da sequência;
- transferência entre guindastes;
- renumeração após alteração;
- simulação antes da persistência;
- comando de reprogramação auditável.

## BUS1660 — EC Console

- recaps de POW, pool, filas, jobs, concluídos, bloqueados, CHEs ativos e push rate;
- integração do quadro operacional com o mapa;
- dados derivados das filas e da telemetria já carregadas pela tela.

## BUS1670 — CHEs no mapa

- posição 2D;
- heading;
- conectividade;
- estado;
- unidade transportada;
- job atual e próximos jobs;
- trilha e rota;
- distinção visual de telemetria stale.

## BUS1680 — Alcances de CHE

- representação circular do alcance;
- edição de range;
- bloqueio quando a telemetria está stale;
- comando persistido para recalcular cobertura e validar dispatch.

## BUS1690 — Filtros e recaps bidirecionais

- filtro por texto, estado, domínio e equipamento;
- destaque dos elementos correspondentes;
- acinzentamento do restante sem retirar o contexto espacial;
- base comum para sincronização entre canvas, listas e indicadores.

## BUS1700 — Workspaces gráficos

- paleta, filtros, atributos, painéis, posições e visibilidade;
- escopos individual, equipe, papel e padrão;
- padrão administrativo restrito a `ADMIN_PORTO`;
- versão imutável a cada salvamento;
- importação e exportação em JSON;
- recuperação pelo backend em outro navegador.

## BUS1710 — Editor de geometria física

- blocos, vias, trilhos, exchange areas, transfer points, reefers e zonas;
- validação de identificadores, tipos, dimensões e ligações;
- estado de rascunho e publicação versionada;
- snapshot integral persistido no comando de publicação.

## BUS1720 — Rotas e congestionamento

- grafo operacional com distância, sentido, bloqueios e congestionamento;
- simulação de interdições;
- cálculo de caminho disponível;
- memória de cálculo com nós, segmentos, custo e ETA;
- bloqueio quando a rede não possui caminho válido.

## BUS1730 — Rail × Yard × Dispatch

- seleção de unidades do pátio;
- atribuição para vagões;
- capacidade, tipo e peso validados;
- exibição de linha, posição, work instruction, CHE e sequência;
- lista explícita de conflitos;
- confirmação integrada por comando único.

## Backend

Foi criado o módulo `operacao2d` no serviço Yard com:

- endpoint para registrar e consultar comandos;
- endpoint para salvar e listar workspaces;
- idempotência por `commandId`;
- snapshot JSON integral;
- usuário, motivo, status e instante;
- versionamento de workspace;
- autorização por perfis operacionais;
- migration `V223__operacao_grafica_2d.sql`.

## Frontend

Arquivos principais:

- `frontend/cloudport/src/components/Operational2DCommandCenter.jsx`;
- `frontend/cloudport/src/operational-2d-tools.js`;
- `frontend/cloudport/src/operational-2d-api.js`;
- `frontend/cloudport/src/operational-2d-tools.css`;
- integração em `VesselPlannerWorkspace.jsx`;
- integração em `OperationalYardViews.jsx`.

## Segurança e integridade

- comandos repetidos não geram nova persistência;
- workspaces nunca sobrescrevem versões anteriores;
- workspace padrão exige `ADMIN_PORTO`;
- payloads críticos são validados por tipo;
- alterações continuam sujeitas às validações definitivas dos domínios operacionais;
- nenhuma confirmação visual substitui lock, versão, permissão ou validação transacional do backend.

## Acessibilidade e ajuda

A central possui manual contextual com:

- finalidade;
- fluxo operacional;
- explicação dos campos;
- permissões;
- estados;
- motivos de bloqueio;
- exemplo;
- atalhos;
- link para o processo completo.

Estados são transmitidos por texto, símbolos e tooltips, sem depender apenas de cor.

## Testes adicionados

Frontend:

- ordenação Stack-wise;
- bloqueio de Paired 20 incompatível;
- divisão e transferência de fila;
- telemetria stale;
- filtros com contexto acinzentado;
- workspace por papel;
- geometria inválida;
- rota alternativa;
- atribuição Rail × Yard.

Backend:

- idempotência por `commandId`;
- nova versão de workspace;
- bloqueio de padrão sem `ADMIN_PORTO`;
- bloqueio de fluxo vazio.
