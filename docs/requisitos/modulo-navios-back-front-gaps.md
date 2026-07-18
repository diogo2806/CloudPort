# Requisitos pendentes - CloudPort

Status: atualizado em 2026-07-18 após a implementação da prova automatizada do corte operacional do monólito modular.

## Instruções obrigatórias para agentes de IA

Este é o backlog funcional e de integração canônico do CloudPort.

Antes de desenvolver, ler este arquivo, `docs/requisitos/requisito-tecnico.md` e `docs/implementados/requisitos-implementados.md`. Depois de desenvolver, remover daqui somente o que foi efetivamente entregue e registrar a implementação no arquivo de requisitos implementados, sem duplicação.

Não registrar neste arquivo tarefas já concluídas, histórico de execução, evidências temporárias, CI/CD em andamento ou itens genéricos sem classe, contrato, fluxo ou critério de aceite identificável.

## Diretriz arquitetural vigente

O backend alvo do CloudPort é um **monólito modular** executado por `backend/cloudport-runtime`.

Os módulos Autenticação, Carga Geral, Gate, Rail, Visibilidade, Yard, Navio e Navio Siderúrgico executam no mesmo processo, preservando limites de domínio, schemas próprios, portas locais e eventos internos. HTTP, mensageria e arquivos permanecem nas bordas de integração ou no caminho de rollback.

O runtime `backend/cloudport-monolito-navio` e os executáveis `servico-*` são alternativas temporárias de rollback e não devem receber novas integrações internas por HTTP.

## Base já entregue

A lista detalhada está em `docs/implementados/requisitos-implementados.md`. Não devem voltar como pendência principal:

1. Control Room com work queues, job lists, dispatch, transições operacionais, Quay Monitor e streaming.
2. Telemetria, dispositivos, comandos remotos, alarmes e indisponibilidades de CHE.
3. Pátio georreferenciado com vistas de bloco, seção, scan, microvisão, heatmaps, workspaces, simulação, notas, restrições, rotas, reefers e editor de allocations.
4. Inventário canônico de unidades e equipamentos, incluindo lacres, documentos, avarias, manutenção, holds, ownership, montagem, reefer e inventário físico.
5. Vessel Planner com profile, top, section e tier views, drag-and-drop, segregação IMDG, restow e overlays operacionais.
6. Gate operacional e visual com múltiplos gates, pistas, filas, appointments, truck visits, inspeções, imagens, documentos, EIR, SLA, trouble transactions e controle de pessoas.
7. Serialização do acesso de pessoas por documento e tradução de disputas concorrentes para `409 Conflict`.
8. Tradução das rejeições transacionais de truck visits para respostas operacionais `409` ou `422`, com SQLStates de domínio e rollback integral.
9. Serialização da geração de faturas e do registro de pagamentos, impedindo cobrança repetida e pagamento acima do saldo.
10. Tradução das colisões concorrentes nos cadastros únicos de carga geral para `409 Conflict`, sem exposição de SQL ou constraints.
11. Ferrovia com visita, manifesto, ordens, composição gráfica, linhas, conflitos, conclusão, partida e transferência de locomotiva para navio.
12. Carga geral, carga de projeto e break-bulk com Bill of Lading, itens, cargo lots, referências, estoque e movimentações.
13. Billing e portal CAP.
14. Grade operacional compartilhada, exportação CSV/Excel protegida contra fórmulas, inspector, filtros, layouts salvos e ajuda contextual.
15. Central global de alertas e line-ups internos e públicos.
16. Processamento EDI assíncrono e idempotente, eventos internos seletivos e contratos versionados.
17. Dockerfiles e parâmetros de implantação do frontend e backend no EasyPanel.
18. Edição de visita e item, conclusão/publicação, invalidação, cancelamento e nova versão de plano, cancelamentos administrativos de visita, item e ordem, compensações e histórico auditável no portal.
19. Sanitização de logs do TOS, segurança standalone da carga geral e autorização dos WebSockets operacionais do Yard.
20. Replanejamento real integrado ao motor de otimização, com mapa e reservas do Yard, restrições de carga, CHEs e work queues, dual-cycling, memória de cálculo, proposta reproduzível, simulação e aplicação revalidada transacionalmente.
21. Replanejamento visual persistido de contêineres entre vagões, com drag-and-drop confirmado por motivo, lock e versão da composição, atualização atômica do manifesto e da ordem ferroviária, auditoria e conflito funcional para alterações concorrentes.
22. Corte operacional comprovável do monólito modular, com exclusão mútua de escrita, instância canônica identificável, jobs e consumidores controlados, verificação dos oito schemas, reinício persistente, integrações de borda e ensaio de retorno ao runtime anterior sem downgrade.

## P0 - Pendências obrigatórias

Não há pendências funcionais P0 comprovadas no código atual. A promoção da revisão no ambiente produtivo e o período de observação seguem o runbook e a governança operacional do ambiente, sem constituir nova implementação de software.

## P1 - Evoluções de integração e dados

### 1. Contratos compartilhados e API pública

1. Adotar `backend/cloudport-contracts` nos módulos que ainda mantêm DTOs e enums equivalentes locais.
2. Remover conversões duplicadas sem quebrar os contratos externos existentes.
3. Gerar tipos TypeScript no pipeline e comparar o snapshot publicado.
4. Criar tela de diagnóstico dos contratos `/api/public/v1/*`.
5. Implementar escopos por cliente, rotação e expiração de segredo, rate limit e auditoria de chamadas.
6. Validar automaticamente rotas, schemas e `operationId` duplicados no OpenAPI consolidado.

### 2. Visibilidade orientada a dados reais

1. Substituir consultas com `findAll()` e filtros em memória por consultas paginadas e indexadas no banco.
2. Persistir movimentos, produtividade, equipamentos alocados e previsão de saída antes de expor esses valores em dashboards.
3. Publicar atualização imediata após eventos de negócio e manter jobs apenas para reconciliação.
4. Criar teste de contexto com PostgreSQL, RabbitMQ e Redis reais, cobrindo redelivery, idempotência e todos os listeners.
5. Relacionar alertas globais à entidade de origem com navegação e histórico auditável consistentes.

### 3. EDI e eventos externos

1. Aplicar VERMAS também às reservas, ordens e validações de capacidade do Yard.
2. Publicar eventos externos específicos de estiva, reserva, ordem, movimento, gate, rail e work queue.
3. Implementar outbox transacional para eventos que atravessam a fronteira da aplicação.
4. Definir política operacional de retenção, quarentena, reprocessamento e descarte de mensagens.
5. Disponibilizar painel unificado de recepção, tentativa, erro, correlação e efeito produzido por mensagem.

### 4. Relatórios operacionais

1. Evoluir relatórios integrados com planejado x realizado, produtividade, divergências e causa raiz.
2. Criar relatórios de gate, pátio, navio, ferrovia, carga geral, inventário, billing e equipamentos usando a mesma infraestrutura.
3. Manter CSV e Excel na grade operacional e adicionar PDF somente para relatórios com layout controlado.
4. Permitir agendamento, armazenamento, download autorizado e retenção configurável.

## P2 - Evoluções avançadas

1. Otimização global Navio + Pátio + Equipamento + Gate + Rail.
2. Comparação automática entre BAPLIE, plano, inventário, execução e posição física.
3. Previsão de gargalos por berço, porão, bloco, fila, pista, linha e equipamento.
4. Cálculo certificado de lashing; o overlay atual é apenas indicador visual de risco.
5. Planejamento integrado de contêiner, carga geral, break-bulk, siderurgia e RoRo no mesmo horizonte de recursos.
6. Controle aduaneiro e documental completo com integração aos órgãos e sistemas aplicáveis.
7. Integração com VMT e sensores reais para despacho, confirmação automática e detecção de divergência.

## Critérios gerais de aceite

1. Todo comando deve exigir autenticação, autorização e motivo quando aplicável.
2. Alterações concorrentes devem produzir resultado determinístico e conflito funcional, nunca `500` previsível.
3. Frontend e backend devem usar o mesmo contrato e a mesma origem de API.
4. Fluxos críticos devem ser idempotentes e auditáveis por `correlationId` e identidade do evento ou comando.
5. O frontend deve exibir estado persistido, não apenas simulação local, quando a ação for confirmada.
6. Jobs e consumidores devem executar em uma única instância autorizada.
7. Integrações externas devem falhar de forma fechada e observável, sem expor payload sensível.
8. Nenhum deployment legado deve ser removido antes da validação de paridade e rollback.

## Fora do escopo imediato

1. Substituição integral de todos os recursos de um TOS comercial.
2. Certificação naval de estabilidade, força estrutural ou lashing sem dados e validação de engenharia apropriados.
3. Automação física de equipamentos sem integração homologada com PLC, ECS, VMT ou fornecedor do equipamento.
