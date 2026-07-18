# Lacunas funcionais remanescentes do CloudPort

## Uso deste documento

Este arquivo registra somente lacunas funcionais e evoluções operacionais que ainda não estão concluídas.

Os requisitos técnicos comprovados por auditoria, com classe, método, situação atual e correção esperada, permanecem em `docs/requisitos/requisito-tecnico.md`.

Os itens concluídos devem ser removidos daqui e registrados em `docs/implementados/requisitos-implementados.md`.

## Estado consolidado

O CloudPort já possui runtime modular, oito schemas, Gate operacional e visual, pátio georreferenciado, inventário canônico, otimização de posições, ferrovia, Vessel Planner gráfico, line-ups, carga geral, break-bulk, bobinas, Control Room, telemetria, alertas, Billing, CAP, grade operacional e deploy no EasyPanel.

Não são mais lacunas:

- tabela operacional com filtros, ordenação, seleção, exportação e inspector;
- mapa do pátio, vistas de bloco, seção, scan e microvisão;
- heatmaps, workspaces, movimentação e restrições gráficas;
- telemetria reefer, alarmes, rotas e allocations;
- Inventory Management canônico;
- composição gráfica de trem e line-up ferroviário;
- profile, top, section e tier no Vessel Planner;
- Gate visual com pistas, filas, jornada, SLA e EIR;
- carga geral e break-bulk;
- controle de entrada e saída de pessoas;
- line-up público de navios;
- fluxo de locomotiva da ferrovia para o navio;
- saída direta de carga autopropelida;
- embarque de contêiner direto do Gate para o navio;
- Billing, CAP e central global de alertas;
- imagens de backend e frontend para EasyPanel.

## P0 — operação e segurança

### 1. Concluir os requisitos técnicos auditados

Tratar os itens vigentes em `docs/requisitos/requisito-tecnico.md`:

- conflito concorrente no controle de pessoas;
- concorrência na geração de faturas e pagamentos;
- tradução das rejeições do banco na abertura de truck visits;
- colisões concorrentes de unicidade em Carga Geral;
- sanitização de respostas do TOS e dos logs do Gate;
- proteção da execução standalone de Carga Geral;
- autenticação e autorização dos WebSockets operacionais do Yard.

### 2. Executar o corte operacional do runtime geral

Ainda falta comprovar em ambiente:

- uma única instância escritora;
- um único scheduler por job;
- um único grupo consumidor por fila;
- validação dos oito históricos Flyway;
- smoke completo de todos os domínios;
- ausência de HTTP entre módulos incorporados;
- rollback ensaiado;
- retirada controlada dos deployments e credenciais legados.

O procedimento está em `docs/operacao-corte-rollback-navio.md`.

### 3. Persistir o replanejamento visual ferroviário

A composição gráfica permite simular o reposicionamento de contêineres entre vagões, mas a alteração ainda não é persistida por um comando operacional dedicado.

Deve ser implementado:

- endpoint transacional de replanejamento;
- validação da visita, manifesto, vagão e contêiner;
- compatibilidade física e operacional do vagão;
- bloqueio concorrente;
- atualização das ordens afetadas;
- auditoria de origem, destino, motivo e operador;
- retorno da composição persistida;
- testes de conflito e idempotência.

## P1 — integração com operação real

### 4. Homologar telemetria e VMT com hardware real

O domínio, os contratos, o heartbeat, os alarmes e o ciclo de comandos já existem. Falta homologação integral com dispositivos reais de campo:

- protocolo e firmware de cada fornecedor;
- autenticação e rotação de credenciais do dispositivo;
- perda e recuperação de conectividade;
- confirmação física da execução do comando;
- latência, ordenação e duplicidade;
- operação degradada e contingência;
- evidência de segurança para comandos remotos.

### 5. Integrar solver certificado de lashing quando exigido

O Vessel Planner exibe overlay visual de risco e utiliza cálculos versionados de estabilidade e resistência. O overlay de lashing não substitui cálculo certificado.

Uma evolução futura pode integrar:

- solver externo homologado;
- versão da regra e do modelo de navio;
- cálculo por stack e bay;
- forças nos componentes de amarração;
- limites certificados;
- memória de cálculo assinada;
- bloqueio de aprovação quando a política operacional exigir certificação externa.

### 6. Evoluir para otimização global multi-recurso

O Yard já utiliza atribuição global de custo mínimo para posições. Ainda não existe um solver único combinando simultaneamente:

- berços;
- guindastes;
- navios;
- blocos e pilhas;
- work queues;
- CHEs;
- trens e linhas ferroviárias;
- Gate e janelas de atendimento;
- prioridades comerciais e operacionais.

A evolução deve preservar restrições duras, explicar decisões e permitir simulação antes da aplicação.

## P2 — evolução operacional

### 7. Persistir workspaces compartilhados

Os workspaces do pátio e as visões nomeadas da grade são persistidos no navegador. Pode ser criado um domínio servidor para:

- workspace privado;
- workspace compartilhado por equipe;
- versão e proprietário;
- permissões;
- filtros, camadas, colunas e posição visual;
- publicação e revogação;
- auditoria.

### 8. Consolidar relatórios executivos e operacionais

Os módulos já possuem indicadores e relatórios específicos. Falta uma camada consolidada com:

- planejado versus realizado;
- produtividade por período e recurso;
- dwell time e ocupação;
- turnaround de caminhões e trens;
- desempenho por navio, berço e guindaste;
- divergências de inventário;
- avarias e indisponibilidades;
- custos e faturamento;
- exportação programada;
- trilha da origem de cada indicador.

### 9. Completar o controle documental e aduaneiro

O CloudPort possui documentos, holds, permissions, ordens, Bill of Lading e integrações operacionais. Uma substituição integral de TOS exige ampliar:

- integrações aduaneiras específicas por país;
- eventos de liberação e bloqueio;
- versionamento documental;
- assinatura e validade;
- reconciliação com manifestos oficiais;
- contingência offline;
- retenção legal e política de descarte.

## Critérios para remover uma lacuna

Um item somente sai deste arquivo quando:

1. backend, banco e contrato necessários estiverem implementados;
2. a interface estiver conectada ao contrato real;
3. autorização e auditoria estiverem aplicadas;
4. concorrência e idempotência estiverem tratadas quando aplicáveis;
5. testes cobrirem o fluxo principal e as rejeições;
6. documentação operacional estiver atualizada;
7. a entrega estiver integrada à `main`.