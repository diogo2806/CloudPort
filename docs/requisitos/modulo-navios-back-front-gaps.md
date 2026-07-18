# Requisitos funcionais e operacionais pendentes - CloudPort

Atualizado em 2026-07-18 após a consolidação das funcionalidades incorporadas à branch `main`.

## Instruções obrigatórias para agentes de IA

Este arquivo deve conter somente lacunas ainda não concluídas.

Depois de desenvolver um item:

1. remover a pendência deste arquivo;
2. registrar a entrega em `docs/implementados/requisitos-implementados.md`;
3. não criar relatórios, históricos ou arquivos paralelos nas pastas `docs/requisitos` e `docs/implementados`.

As pendências técnicas comprovadas por auditoria, com classe e método, permanecem em `docs/requisitos/requisito-tecnico.md`.

## Diretriz arquitetural vigente

O backend alvo é o monólito modular executado por `backend/cloudport-runtime`.

Não criar novos microsserviços para funcionalidades internas nem introduzir chamadas HTTP entre módulos que já executam no mesmo processo. Integrações externas continuam na borda por HTTP, EDI, RabbitMQ, storage ou protocolos próprios.

`backend/cloudport-monolito-navio` e os executáveis standalone permanecem somente para rollback controlado.

## 1. Pendências técnicas auditadas

Os itens abaixo continuam pendentes e estão detalhados em `docs/requisitos/requisito-tecnico.md`:

| ID | Pendência |
| --- | --- |
| `ERR10` | serializar entradas e saídas concorrentes de pessoas e retornar conflito funcional |
| `ERR20` | serializar geração de faturas e pagamentos concorrentes |
| `ERR30` | traduzir rejeições transacionais da abertura de truck visits para `409` ou `422` |
| `ERR40` | tratar concorrência nos cadastros únicos de carga geral sem expor constraint ou SQL |
| `SEC70` | retirar payload externo e dados operacionais sensíveis dos logs do Gate |
| `SEC80` | proteger a execução standalone de Carga Geral com autenticação e autorização por operação |
| `SEC90` | autenticar handshake e assinatura dos WebSockets operacionais do Yard |

Esses itens somente podem sair daqui quando o código, o contrato HTTP e os testes correspondentes estiverem implementados.

## 2. Navio e Vessel Planner

1. Expor no frontend a conclusão e publicação formal do plano por `POST /visitas-navio/{id}/plano-estiva/{planoId}/concluir`.
2. Completar a edição de visita e item pelos contratos `PUT /visitas-navio/{id}` e `PUT /visitas-navio/{id}/itens/{itemId}`.
3. Criar cancelamento administrativo diferenciado para visita, item, plano e operação já iniciada.
4. Evoluir o relatório integrado para planejado versus realizado, produtividade, divergências detalhadas e exportação operacional.
5. Integrar validações certificadas de lashing, visibility, estabilidade e resistência quando o terminal exigir aprovação oficial por modelo de navio.
6. Manter a memória de cálculo e a origem dos dados estruturais rastreáveis até a versão do navio utilizada na aprovação.

## 3. Otimização global

1. Evoluir a otimização local já incorporada para um motor global Navio, Yard, Gate, Rail e equipamentos.
2. Considerar simultaneamente ETA, ETB, ETD, cutoff, dual-cycling, rehandles, capacidade de pilhas, disponibilidade de CHE, prioridades e restrições operacionais.
3. Comparar cenários antes da confirmação e explicar os fatores que determinaram a posição ou sequência escolhida.
4. Prever gargalos por berço, porão, bloco, fila, linha ferroviária, pista e equipamento.
5. Permitir reotimização controlada sem invalidar ordens em execução nem perder auditoria do plano anterior.

## 4. Integrações externas e EDI

1. Aplicar os efeitos de COPRAR, COARRI e VERMAS às reservas, ordens, capacidade e planejamento quando o contrato operacional exigir atualização automática.
2. Publicar eventos externos específicos de estiva, reserva, ordem, movimento, work queue, Gate, Rail e carga geral, sem expor eventos internos do monólito.
3. Implementar outbox transacional para eventos externos que precisem de entrega garantida além da persistência e retentativa já existentes.
4. Criar tela operacional para quarentena, diagnóstico e reprocessamento EDI com filtros e comparação entre tentativa original e reprocessamentos.
5. Criar tela de diagnóstico dos contratos `/api/public/v1/*`, incluindo cliente, escopo, expiração, rate limit e últimas falhas autorizadas.
6. Concluir rotação de segredo e auditoria por cliente ou aplicação da API pública.

## 5. Visibilidade e streaming

1. Expandir eventos e streaming versionado para todos os domínios operacionais, preservando autorização por tópico e baixa cardinalidade nas métricas.
2. Substituir consultas amplas e filtros em memória remanescentes por paginação e filtros no banco.
3. Persistir todos os indicadores exibidos quando ainda forem derivados de aproximações ou agregações incompletas.
4. Consolidar uma linha temporal única do ativo entre Gate, Yard, Rail, Navio, armazém e cliente.
5. Permitir investigação de divergência com acesso aos eventos, correlações, documentos e comandos que alteraram o estado.

## 6. Gate, pessoas e CAP

1. Concluir o tratamento funcional das disputas de capacidade, acesso, entrada e saída descritas em `ERR10`, `ERR20` e `ERR30`.
2. Garantir que todas as ações sensíveis do Gate visual usem autorização por operação e motivo obrigatório.
3. Integrar dispositivos reais de OCR, balança, cancela, biometria e impressora, mantendo simuladores somente como contingência.
4. Evoluir o CAP para autosserviço completo da transportadora conforme as políticas comerciais e de segurança do terminal.
5. Completar controle documental e aduaneiro quando houver integração oficial com os órgãos e sistemas externos responsáveis.

## 7. Rail

1. Integrar telemetria real de locomotivas, vagões, sensores de via e equipamentos de atendimento.
2. Consolidar conflitos entre line-up ferroviário, capacidade das linhas, recursos e janelas de navio.
3. Implementar confirmação operacional por vagão e reconciliação automática com o inventário físico.
4. Integrar ordens ferroviárias externas e mensagens de chegada, composição, carga, descarga e partida.

## 8. Yard e Equipment Control

1. Integrar VMT e telemetria real dos CHEs com confirmação de execução, posição e indisponibilidade.
2. Validar mensagens de equipamento contra a work instruction ativa antes de avançar o estado.
3. Implementar modo degradado para perda de telemetria sem permitir conclusão silenciosa de movimentos.
4. Persistir e comparar o mapa físico levantado com o mapa lógico, registrando divergências de inventário por posição.
5. Evoluir rotas e interdições para considerar sentido de via, prioridade, congestionamento, dimensões do equipamento e zonas de segurança.

## 9. Inventory Management e carga geral

1. Concluir o tratamento de concorrência dos cadastros únicos descrito em `ERR40`.
2. Proteger o standalone de Carga Geral conforme `SEC80`.
3. Integrar ordens comerciais, documentos aduaneiros e liberações externas quando aplicáveis.
4. Evoluir inventário físico para campanhas, contagem cega, recontagem, aprovação e ajuste segregado por perfil.
5. Integrar manutenção e reparo com ordens de serviço, custos, peças e indisponibilidade do equipamento.
6. Evoluir carga geral para planejamento gráfico de armazém, área descoberta e posição a bordo quando o tipo de carga exigir.

## 10. Segurança e proteção de dados

1. Concluir `SEC70`, `SEC80` e `SEC90` antes de disponibilizar os respectivos fluxos em ambiente externo.
2. Aplicar autorização por destino nos canais SSE e WebSocket de cada domínio.
3. Restringir origens WebSocket pela configuração CORS canônica.
4. Garantir que logs, erros, métricas e tracing não incluam tokens, credenciais, payload EDI integral, dados aduaneiros ou dados pessoais desnecessários.
5. Definir retenção, acesso e descarte para documentos, imagens, telemetria e auditorias.
6. Revisar permissões dos módulos standalone mantidos para rollback e falhar fechado quando a configuração obrigatória estiver ausente.

## 11. Corte operacional e rollback

1. Executar o corte dos ambientes para o `cloudport-runtime` como única instância escritora.
2. Garantir uma única execução de jobs e consumidores durante a transição.
3. Validar paridade dos endpoints de Autenticação, Gate, Rail, Visibilidade, Yard, Navio e Carga Geral no runtime canônico.
4. Validar falhas e tempos limite de TOS, OCR, EDI, RabbitMQ, Redis, storage e demais adaptadores externos.
5. Executar smoke completo da imagem com PostgreSQL, RabbitMQ e Redis reais ou equivalentes de homologação.
6. Ensaiar retorno para o runtime de rollback sem downgrade do banco.
7. Remover deployments, imagens, credenciais e variáveis legadas somente após aceite e período de observação.

## 12. Critérios de aceite remanescentes

1. Nenhuma operação concorrente conhecida deve produzir `500`, duplicidade ou estado parcial.
2. Nenhum canal operacional deve permitir conexão ou assinatura anônima.
3. Nenhum módulo standalone preservado para rollback deve ficar desprotegido.
4. Todos os comandos sensíveis devem registrar usuário, motivo, origem e `correlationId`.
5. O frontend deve consumir uma única origem de API.
6. Módulos incorporados não devem realizar chamadas HTTP entre si.
7. Eventos externos devem ser versionados, idempotentes e rastreáveis.
8. O plano operacional deve ser reproduzível a partir das versões dos dados usadas no cálculo.
9. O corte deve manter uma única instância escritora, um executor de cada job e um consumidor efetivo por fila.
10. O rollback deve trocar binário e roteamento sem reverter automaticamente migrations.

## Fora do escopo imediato

1. Substituição integral de um TOS comercial.
2. Certificação marítima oficial dos cálculos sem dados homologados do navio.
3. Automação física de equipamentos sem integração com o fabricante ou ECS.
4. Integrações aduaneiras sem contrato e credencial dos órgãos responsáveis.
