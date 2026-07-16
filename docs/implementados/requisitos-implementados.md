# Requisitos implementados - CloudPort

## Instruções obrigatórias para agentes de IA

Esta pasta deve manter um único arquivo: `docs/implementados/requisitos-implementados.md`.

Não criar outros documentos, arquivos de evidência, logs, históricos ou rascunhos nesta pasta. Toda entrega deve sair de `docs/requisitos/modulo-navios-back-front-gaps.md` e ser registrada aqui sem duplicação.

## Módulo Navio implementado

1. Criar visita de navio.
2. Criar item operacional de embarque, descarga e restow.
3. Criar plano de estiva por visita.
4. Criar eventos e resumo operacional da visita.
5. Criar endpoints básicos `/visitas-navio`.
6. Criar endpoints de integração em `/visitas-navio/{id}/integracao-patio`.
7. Adicionar campos de integração em `ItemOperacaoNavio`.
8. Expor relatório operacional integrado básico.

## Integração Navio + Yard implementada

1. Criar reserva de pátio vinculada ao item de navio.
2. Adicionar visita, item e plano em `OrdemTrabalhoPatio`.
3. Criar ordem real no Yard.
4. Impedir ordem ativa duplicada por visita e item.
5. Expor filas e ordens sem cobertura por visita.
6. Permitir sincronização manual e automática de status.
7. Permitir gerar reservas e ordens.
8. Permitir replanejamento inicial.
9. Permitir alterar prioridade, suspender e retomar ordens.
10. Atualizar item conforme o estado real da ordem.
11. Preencher posição real, consumir reserva ao concluir e cancelar reserva ao cancelar ordem.
12. Registrar evento somente quando a reconciliação altera dados.

## Control Room implementado

1. Criar painel Navio + Yard com filtros, movimentos iminentes, filas, reservas, ordens, alertas e exceções.
2. Permitir gerar reservas e ordens, sincronizar, replanejar, priorizar, suspender e retomar.
3. Carregar work queues persistentes com job list expansível.
4. Ativar ou desativar fila, editar POW, pool e equipamento, executar dispatch, resetar e cancelar work instruction.
5. Exibir loading por ação e feedback de sucesso ou erro.
6. Integrar ao portal pela rota autenticada `/home/navio/control-room`.
7. Implementar SSO por `postMessage` restrito a origens configuradas.
8. Implementar login próprio como fallback e restringir perfis.
9. Enviar JWT, usuário, origem e `X-Correlation-Id` nas ações.
10. Exibir erro com `codigo`, `mensagem`, `detalhes` e `correlationId`.
11. Executar consultas do snapshot em paralelo, aplicar resultado atomicamente e impedir sobreposição.
12. Solicitar motivo antes de alterações de fase, prioridade, suspensão, retomada, reset, cancelamento e alterações administrativas de work queue.

## Work queues implementadas

1. Listar, criar, ativar e desativar work queue.
2. Associar POW, pool operacional e equipamento.
3. Expor job list e executar dispatch.
4. Resetar e cancelar work instruction.
5. Expor work queues pelo módulo de Navio.
6. Persistir `workQueueId` em `OrdemTrabalhoPatio`.
7. Atualizar job list por `PATCH /yard/patio/work-queues/{id}/ordens`.
8. Vincular automaticamente somente quando houver uma fila compatível inequívoca.
9. Remover comparação incorreta entre camada e bloco/zona.
10. Honrar `limiteOrdens` no dispatch.
11. Padronizar a resposta de dispatch.
12. Auditar criação, ativação, desativação, POW, pool, equipamento, vínculo, dispatch, reset e cancelamento.
13. Restringir operações por perfil.
14. Exigir e auditar motivo em ativação, desativação, alteração de POW, pool, equipamento, job list, reset e cancelamento.
15. Exigir e auditar motivo em alteração de status, prioridade, suspensão e retomada de ordens.

## Reserva contra mapa real implementada

1. Consultar `GET /yard/patio/reservas/posicoes` antes de reservar.
2. Selecionar posição real com linha, coluna e camada.
3. Recusar mapa vazio, posição inexistente, posição ocupada e reserva ativa duplicada.
4. Remover identificadores artificiais de posição.
5. Armazenar identificador e coordenadas reais.
6. Garantir dados necessários à criação da ordem real.
7. Validar bloqueio, interdição, área permitida, tipo de carga, peso, altura, camada e capacidade da pilha.
8. Expirar reservas por prazo configurável.
9. Cancelar reserva ao cancelar visita ou replanejar item.
10. Auditar criação, consumo, cancelamento e expiração.
11. Compensar a reserva anterior durante replanejamento.

## Autenticação e segurança implementadas

1. Preservar a senha digitada sem remover caracteres.
2. Não armazenar senha no `localStorage`.
3. Armazenar somente dados seguros da sessão.
4. Autenticar integrações legadas por `X-CloudPort-Service-Key`.
5. Comparar credencial interna em tempo constante.
6. Aplicar roles de serviço separadas.
7. Restringir manutenção do cadastro canônico.
8. Liberar cabeçalhos de correlação no CORS.
9. Retornar `503` quando uma integração obrigatória falhar, sem mascarar como lista vazia.
10. Proteger `/api/public/v1/**` por cliente ou aplicação usando `X-CloudPort-Client-Id` e `X-CloudPort-Client-Secret`.
11. Comparar o segredo do cliente externo em tempo constante e associar a role `INTEGRACAO_EXTERNA`.

## Scheduler operacional implementado

1. Remover dados aleatórios de equipamentos, contêineres e coordenadas.
2. Exigir requisição com navio, equipamentos e posições reais.
3. Validar quantidades manifestadas.
4. Validar janela de chegada e partida.
5. Considerar conflito somente no mesmo berço.
6. Preservar duração ao deslocar slot.
7. Persistir agenda em `vessel_schedule`.
8. Calcular diagnóstico por movimentos planejados reais.
9. Restringir a API por perfil.

## Cadastro canônico de navios implementado

1. Definir Navio como fonte dos dados comuns.
2. Vincular `NavioSiderurgico` por `navioCadastroId` único.
3. Resolver cadastro por ID ou IMO.
4. Manter localmente somente a projeção operacional siderúrgica.
5. Sincronizar a projeção com o cadastro canônico.

## Monólito modular CloudPort implementado no código

### Runtime e módulos

1. Criar o runtime `backend/cloudport-monolito-navio`.
2. Incorporar Navio e Navio Siderúrgico.
3. Incorporar Yard.
4. Incorporar Gate e Rail.
5. Incorporar Autenticação e Visibilidade.
6. Manter os diretórios `servico-*` como módulos compiláveis isoladamente para rollback.
7. Produzir um único JAR executável e uma única imagem Docker.
8. Incorporar o frontend React do Control Room ao JAR.
9. Expor `GET /assets/configuracao.json` dinamicamente.

### Comunicação interna por portas

1. Extrair `CadastroNavioPorta` e implementar `CadastroNavioLocalAdapter`.
2. Manter `NavioCadastroCliente` somente como adaptador HTTP legado.
3. Transformar `OrdemPatioYardCliente` em porta e criar `OrdemPatioLocalAdapter`.
4. Transformar `PosicaoPatioYardCliente` em porta e criar `PosicaoPatioLocalAdapter`, preservando restrições e capacidade do mapa real.
5. Transformar `ClienteStatusPatio` em porta e criar `StatusPatioLocalAdapter` para Gate → Yard.
6. Transformar `AutenticacaoClient` em porta e criar `AutenticacaoLocalAdapter` para Gate → Autenticação.
7. Registrar adaptadores HTTP somente quando a propriedade de integração estiver em `http`.
8. Configurar Navio, Yard e Autenticação em modo `local` no runtime.
9. Impedir por ArchUnit que o runtime dependa de classes `*HttpAdapter`.

### Maven e empacotamento

1. Evoluir `backend/cloudport-navio-modules` para parent e reator Maven comum.
2. Centralizar Java, versões, BOMs, `dependencyManagement`, `pluginManagement` e Enforcer.
3. Incluir os sete módulos e o runtime no reator.
4. Permitir JAR de biblioteca pelo perfil `modulo-monolito` e preservar execução standalone.
5. Remover inclusão direta de fontes de projetos irmãos.
6. Publicar recursos e migrações dentro do artefato proprietário.
7. Atualizar Dockerfile para copiar e compilar todos os módulos pelo reator.
8. Incluir `cloudport-contracts` no reator Maven, no workflow e nas imagens Docker standalone e consolidada.
9. Publicar um único OpenAPI no runtime consolidado com segurança JWT e credenciais de cliente externo.
10. Garantir `operationId` único no OpenAPI consolidado.

### Schemas, ownership e Flyway

1. Usar uma conexão PostgreSQL e sete schemas proprietários:
   - `cloudport_navio`;
   - `cloudport_siderurgico`;
   - `cloudport_yard`;
   - `cloudport_gate`;
   - `cloudport_rail`;
   - `cloudport_autenticacao`;
   - `cloudport_visibilidade`.
2. Definir como proprietário o módulo que publica a migração que cria a estrutura.
3. Publicar migrações em `cloudport/migrations/<modulo>`.
4. Criar um objeto Flyway e um `flyway_schema_history` por schema.
5. Executar todos os Flyway antes do `EntityManagerFactory`.
6. Validar nomes de schema.
7. Habilitar `validateOnMigrate`, desabilitar `clean` e criar schemas quando necessário.
8. Configurar o `search_path` com os sete schemas e `public`.
9. Preservar rollback por estratégia `expand and contract`, sem downgrade automático.
10. Documentar ownership, compatibilidade e regras destrutivas.

### Infraestrutura transversal centralizada

1. Centralizar uma cadeia de segurança stateless.
2. Incorporar login e emissão de token do módulo Autenticação.
3. Centralizar JWT, roles, CORS e credencial interna transitória.
4. Centralizar Jackson com Java Time, UTC e propriedades não nulas.
5. Publicar OpenAPI consolidado.
6. Centralizar tratamento de erros com código, mensagem, detalhes, status, caminho, timestamp e `correlationId`.
7. Criar filtro de `X-Correlation-Id` e `traceId` no MDC.
8. Criar métrica HTTP central e exportação Prometheus.
9. Centralizar padrão de logs.
10. Centralizar scheduler e seu tratamento de erro.
11. Centralizar cliente HTTP para integrações externas.
12. Centralizar conversor JSON principal do RabbitMQ.
13. Excluir do runtime configurações standalone duplicadas de segurança, erros, OpenAPI, observabilidade e conversores genéricos.

### Execução única e coexistência

1. Controlar escrita por `cloudport.runtime.writes-enabled`.
2. Retornar `503` para comandos de escrita no runtime desabilitado.
3. Controlar jobs por `cloudport.runtime.jobs-enabled`.
4. Controlar consumidores por `cloudport.runtime.consumers-enabled` e `auto-startup` do RabbitMQ.
5. Manter monólito como escritor, scheduler e consumidor ativo.
6. Manter legados sem escrita, jobs e consumidores durante coexistência.
7. Serializar jobs críticos por `pg_try_advisory_xact_lock`.
8. Adicionar PostgreSQL, RabbitMQ e Redis ao Compose consolidado.
9. Manter deployments e credenciais legadas até validar paridade e rollback.

### Testes e proteção arquitetural

1. Criar teste de contexto com PostgreSQL 16 em Testcontainers.
2. Validar os sete schemas e históricos Flyway.
3. Validar ausência de migrações pendentes.
4. Validar portas locais e ausência dos adaptadores HTTP no contexto.
5. Validar uma única cadeia de segurança.
6. Validar controllers incorporados no mesmo contexto.
7. Testar exclusão mútua por advisory lock.
8. Criar testes ArchUnit contra ciclos.
9. Impedir dependência de módulo para o runtime.
10. Impedir acesso direto ao repository de outro módulo.
11. Impedir uso de adaptador HTTP pelo runtime.
12. Validar build da imagem com todos os módulos.

### Documentação e operação

1. Registrar monólito modular como arquitetura alvo.

## Idempotência dos consumidores de Visibilidade implementada

1. Exigir `eventId` ou `messageId` nos eventos conhecidos de Yard, Gate, Rail e Navio.
2. Registrar a identidade, o tipo e o hash canônico do payload em `visibilidade_evento_processado`.
3. Inserir a identidade com unicidade no PostgreSQL antes de aplicar o efeito.
4. Executar deduplicação, atualização da projeção e gravação do histórico na mesma transação.
5. Ignorar redelivery com a mesma identidade e o mesmo payload sem reaplicar o efeito.
6. Rejeitar colisão de identidade quando o tipo ou o payload forem divergentes.
7. Vincular `HistoricoMovimento.eventoId` ao evento externo e impedir histórico duplicado por índice único.
8. Reverter a identidade persistida quando o efeito falhar, permitindo retentativa segura.
9. Cobrir primeira entrega, redelivery, colisão, envelope inválido e propagação de falha por testes unitários.
