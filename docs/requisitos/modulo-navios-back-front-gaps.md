# Requisitos funcionais e de integração pendentes — CloudPort

Status: atualizado em 2026-07-18 com base na `main` após as entregas de interface operacional, inventário, carga geral, Gate completo, Control Room e implantação no EasyPanel.

## Organização da documentação

Esta pasta mantém duas referências complementares:

- `modulo-navios-back-front-gaps.md`: lacunas funcionais, de integração e de operação;
- `requisito-tecnico.md`: pendências técnicas comprovadas por auditoria de código.

Itens concluídos devem sair deste arquivo e ser registrados em `docs/implementados/requisitos-implementados.md`. Não duplicar aqui recursos já entregues.

## Diretriz arquitetural vigente

O backend alvo é o monólito modular `backend/cloudport-runtime`, com oito módulos incorporados:

1. Autenticação;
2. Carga Geral;
3. Gate;
4. Rail;
5. Visibilidade;
6. Yard e Inventário;
7. Navio;
8. Navio Siderúrgico.

`backend/cloudport-monolito-navio` e os executáveis `servico-*` permanecem somente para rollback e coexistência controlada. Novas funcionalidades internas não devem introduzir HTTP entre módulos executados no mesmo processo.

## Capacidades já entregues que não devem voltar como pendência

- grade operacional com busca, filtros, paginação, seleção, inspector, CSV e Excel seguro;
- ajuda contextual em todas as páginas que utilizam o cabeçalho compartilhado;
- ferrovia visual com composição, vagões, linhas, ocupação, progresso e conflitos;
- Control Room com equipamentos, telemetria, dispositivos, comandos, alarmes e indisponibilidades;
- pátio com bloco, seção, scan, microvisão, heatmaps, CHEs, rotas, reefers, restrições, notas, workspaces, movimentação e allocations;
- Vessel Planner com profile, top, section e tier views, drag-and-drop, pesos, IMDG, restow, guindastes e overlays técnicos;
- inventário canônico de unidades e equipamentos;
- Gate visual e domínio operacional completo;
- carga geral, carga de projeto e break-bulk;
- Billing/CAP e central global de alertas;
- Dockerfiles de backend e frontend compatíveis com os contextos do EasyPanel.

## 1. Pendências técnicas auditadas

A implementação e os critérios detalhados estão em `docs/requisitos/requisito-tecnico.md`.

| ID | Pendência |
| --- | --- |
| `ERR10` | serializar entrada e saída concorrentes de pessoas e retornar conflito funcional |
| `ERR20` | serializar geração de faturas e registro de pagamentos |
| `ERR30` | traduzir rejeições transacionais de abertura de truck visit |
| `ERR40` | tratar concorrência nos cadastros únicos de carga geral |
| `SEC70` | remover payloads externos e dados sensíveis dos logs do Gate |
| `SEC80` | proteger a execução standalone de Carga Geral |
| `SEC90` | autenticar e autorizar handshakes e assinaturas WebSocket do Yard |

Esses itens não devem ser removidos sem alteração de código comprovada e nova auditoria da `main`.

## 2. Navio e Vessel Planner

1. Expor no frontend a conclusão/publicação do plano por `POST /visitas-navio/{id}/plano-estiva/{planoId}/concluir`.
2. Completar edição de visita e item pelos contratos `PUT /visitas-navio/{id}` e `PUT /visitas-navio/{id}/itens/{itemId}`.
3. Criar cancelamento administrativo diferenciado para visita e item, com motivo, autorização e auditoria.
4. Criar diagnóstico operacional dos contratos `/api/public/v1/*`, incluindo disponibilidade, escopo, autenticação e último evento entregue.
5. Integrar o motor matemático global ao replanejamento de visita, considerando ETA, ETB, ETD, cutoff, disponibilidade de equipamentos, rehandle e dual-cycling.
6. Evoluir o relatório integrado com planejado x realizado, produtividade, divergências detalhadas e exportação PDF.
7. Substituir o indicador visual de lashing por cálculo certificado quando o produto exigir aprovação operacional formal. O overlay atual é indicativo.
8. Validar estabilidade, força estrutural e lashing com dados homologados de cada classe de navio antes de tratar os resultados como certificação marítima.

## 3. Yard e Inventário

1. Conectar a otimização global ao editor de allocations e ao replanejamento gráfico, mantendo a simulação antes da confirmação.
2. Integrar VGM/VERMAS às reservas, ordens, capacidade de pilha e validações de posição quando aplicável.
3. Criar inventário físico móvel/offline com sincronização de divergências para operações de campo sem conectividade contínua.
4. Validar os canais WebSocket conforme `SEC90`; SSE autenticado não elimina a necessidade de proteger STOMP.
5. Integrar telemetria de CHE e reefer aos dispositivos reais do terminal, com protocolo, qualidade, relógio e política de dados homologados por ambiente.
6. Adicionar trilha de aprovação para mudanças massivas de allocations, restrições e bloqueios.

## 4. Gate

1. Concluir `ERR10`, `ERR20`, `ERR30` e `SEC70` antes de classificar os fluxos concorrentes e integrações como resilientes em produção.
2. Criar e2e de appointment, abertura de truck visit, múltiplas transações, trouble, inspeção, documentos e impressão/reimpressão de EIR.
3. Integrar OCR, balança, câmera e dispositivos de pista reais, com timeout, retentativa, reconciliação e modo degradado.
4. Garantir reserva e liberação transacional de capacidade de janelas em todos os caminhos de cancelamento, rejeição e rollback.
5. Completar matriz de autorização por estágio, pista, facility e operação de override.

## 5. Ferrovia

1. Persistir o replanejamento visual de contêineres entre vagões; a entrega atual mantém essa ação como simulação no frontend.
2. Criar endpoint transacional para alocação, remoção e troca de unidade em vagão, validando capacidade, compatibilidade, bloqueio e conflito de visita.
3. Integrar ocupação das linhas com intertravamento, sinalização ou fonte operacional homologada quando disponível.
4. Criar planejamento de recursos por trem com CHE, equipe, janela e conflito de uso compartilhado.
5. Cobrir o fluxo ferroviário por e2e, incluindo chegada, operação, bloqueio, replanejamento e partida.

## 6. Carga Geral e Break-Bulk

1. Concluir `ERR40` e `SEC80`.
2. Integrar planejamento gráfico de armazém, áreas abertas, porões e posições de carga não conteinerizada.
3. Criar plano de estivagem e peação específico para produtos siderúrgicos, projeto e break-bulk quando não representáveis pelo Vessel Planner de contêineres.
4. Adicionar inventário físico por unidade de embalagem, marca, lote e localização detalhada.
5. Integrar pesagem, medição, inspeção e documentação externa com reconciliação.
6. Criar relatórios operacionais de saldo, avaria, produtividade, carga parcial e divergência de manifesto.

## 7. Control Room e Visibilidade

1. Criar e2e para login/SSO, SSE, job list, dispatch, transições, motivo obrigatório, comandos de dispositivo e indisponibilidade do Yard.
2. Consolidar drill-down único ligando alerta, equipamento, work instruction, reserva, item de navio, movimento e auditoria.
3. Permitir filtros e workspaces compartilhados por equipe, além dos workspaces locais do navegador.
4. Homologar os comandos remotos com dispositivos reais e definir confirmação, expiração, cancelamento e resposta tardia.
5. Paginar no banco consultas de alto volume ainda executadas em memória, quando identificadas por profiling.
6. Criar retenção, arquivamento e purge para telemetria, comandos, eventos e histórico de alertas.
7. Criar regras de escalonamento, notificação e SLA para alertas críticos.

## 8. Contratos, EDI e integrações externas

1. Adotar `backend/cloudport-contracts` nos módulos que ainda mantêm DTOs e enums externos equivalentes.
2. Gerar tipos TypeScript no pipeline a partir do OpenAPI consolidado e falhar em quebra incompatível.
3. Adicionar escopos, rotação de segredo, expiração, rate limit e auditoria por cliente da API pública.
4. Validar automaticamente ausência de rota, schema e `operationId` duplicados.
5. Publicar eventos externos específicos de estiva, reserva, ordem, movimento, Gate, Rail e carga geral, sem expor eventos internos do monólito.
6. Implementar outbox persistente para eventos externos que não possam ser perdidos entre commit e publicação.
7. Integrar BAPLIE, COPRAR, COARRI e VERMAS aos efeitos operacionais completos de Yard, Gate e Navio.
8. Cobrir filas de quarentena, reprocessamento e colisão idempotente por testes de integração com infraestrutura real.

## 9. Corte, implantação e rollback

1. Executar o corte operacional mantendo somente uma instância escritora, um executor de cada job e um grupo consumidor por fila.
2. Validar os oito schemas e históricos Flyway no ambiente de destino.
3. Executar smoke completo do runtime com PostgreSQL, RabbitMQ, Redis e integrações externas simuladas.
4. Validar os builds Docker nos dois contextos suportados: raiz do repositório e `/backend` no EasyPanel.
5. Validar o frontend no contexto `/frontend`, porta `80`, health `/health` e fallback de SPA.
6. Ensaiar rollback para `cloudport-monolito-navio` e, quando necessário, para serviços isolados.
7. Remover imagens, deployments, chaves internas e variáveis legadas somente após período de observação e aprovação formal.

## Critérios de aceite pendentes

1. Nenhuma pendência técnica auditada permanece aberta para o fluxo promovido.
2. Replanejamentos visuais que alteram operação são persistidos e auditados.
3. Integrações com dispositivos possuem timeout, idempotência, reconciliação e modo degradado.
4. Eventos externos críticos usam outbox ou garantia equivalente.
5. Testes de service, controller, contrato, integração e frontend cobrem os fluxos críticos.
6. O runtime canônico é a única origem de API e não registra chamadas HTTP internas entre módulos.
7. Jobs, consumidores e escritas executam em uma única instância durante coexistência e corte.
8. Os oito históricos Flyway estão válidos e o rollback foi ensaiado sem downgrade de banco.
9. Logs não expõem payloads, credenciais, dados aduaneiros ou dados pessoais.
10. Canais HTTP, SSE e WebSocket exigem autenticação e autorização compatíveis com o dado entregue.

## Fora do escopo do estado atual

- substituição integral de um TOS comercial;
- certificação marítima automática sem dados homologados de estabilidade, força e lashing;
- integração com hardware de terminal sem contrato e ambiente de homologação;
- motor matemático global multi-recurso sem dados operacionais confiáveis.
