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
6. Criar endpoints básicos de integração em `/visitas-navio/{id}/integracao-patio`.
7. Adicionar campos de integração em `ItemOperacaoNavio`.
8. Expor relatório operacional integrado básico.

## Integração Navio + Pátio implementada

1. Criar reserva de pátio vinculada ao item de navio.
2. Adicionar campos de visita, item e plano em `OrdemTrabalhoPatio`.
3. Criar ordem real no `servico-yard` por `/yard/patio/ordens/navio`.
4. Impedir ordem ativa duplicada por `visitaNavioId + itemOperacaoNavioId`.
5. Expor filas e ordens sem cobertura por visita no Yard e no módulo de navio.
6. Permitir sincronização manual de status.
7. Permitir gerar reservas e ordens de pátio.
8. Permitir replanejamento inicial.
9. Permitir alterar prioridade, suspender e retomar ordens.
10. Reconciliar automaticamente visitas ativas por job agendado.
11. Atualizar item para `EM_MOVIMENTO`, `OPERADO`, `BLOQUEADO` ou `CANCELADO` conforme a ordem real.
12. Preencher posição real, consumir reserva ao concluir e cancelar reserva ao cancelar ordem.
13. Registrar eventos somente quando a reconciliação alterar dados, evitando histórico vazio repetitivo.

## Control Room implementado

1. Criar painel Angular Navio + Pátio com filtros, movimentos iminentes, filas, reservas, ordens, alertas e exceções.
2. Permitir gerar reservas, gerar ordens, sincronizar, replanejar, priorizar, suspender e retomar ordens.
3. Carregar e renderizar work queues persistentes com job list expansível.
4. Permitir ativar/desativar fila, editar POW/pool/equipamento, executar dispatch, resetar e cancelar work instruction.
5. Exibir loading por ação e feedback de sucesso/erro.
6. Integrar o módulo ao portal principal pela rota autenticada `/home/navio/control-room`.
7. Implementar SSO entre o portal e o módulo por `postMessage` restrito a origens configuradas.
8. Implementar login próprio como fallback e limitar acesso aos perfis operacionais.
9. Enviar JWT, usuário autenticado, origem da ação e `X-Correlation-Id` nas chamadas operacionais.
10. Exibir `codigo`, `mensagem`, `detalhes` e `correlationId` quando retornados pelo backend.
11. Executar as consultas do snapshot em paralelo, aplicar o resultado de forma atômica e impedir atualizações sobrepostas.

## Work queues implementadas

1. Listar, criar, ativar e desativar work queue.
2. Associar POW, pool operacional e equipamento.
3. Expor job list e executar dispatch.
4. Resetar e cancelar work instruction.
5. Expor work queues pelo módulo de navio.
6. Persistir `workQueueId` em `OrdemTrabalhoPatio`.
7. Atualizar explicitamente a job list por `PATCH /yard/patio/work-queues/{id}/ordens`.
8. Vincular automaticamente uma ordem apenas quando existir uma única fila compatível, evitando associação ambígua.
9. Remover a comparação incorreta entre camada de destino e bloco/zona.
10. Honrar `limiteOrdens` no dispatch.
11. Padronizar a resposta com `workQueueId`, `totalOrdensDespachadas`, `totalOrdensIgnoradas`, `ordens` e `mensagem`.
12. Persistir auditoria de criação, ativação, desativação, POW/pool, equipamento, vínculo, dispatch, reset e cancelamento.
13. Restringir operações a perfis autorizados e ao serviço interno de navios.

## Reserva contra mapa real implementada

1. Consultar `GET /yard/patio/posicoes` antes de reservar.
2. Selecionar somente posição real com linha, coluna e camada.
3. Recusar mapa vazio, posição inexistente, posição ocupada e posição com reserva ativa.
4. Remover a geração de identificadores artificiais como `V{visita}-D-{sequencia}`.
5. Armazenar identificador e coordenadas reais na reserva.
6. Garantir que a reserva gerada contenha os dados exigidos para criar a ordem real no Yard.

## Autenticação e segurança implementadas

1. Preservar a senha digitada sem remover caracteres antes do login.
2. Não armazenar senha no `localStorage`.
3. Armazenar no portal somente os dados seguros da sessão.
4. Autenticar chamadas entre `servico-navio-siderurgico`, `servico-yard` e `servico-navio` com `X-CloudPort-Service-Key`.
5. Usar comparação constante da credencial interna.
6. Aplicar roles de serviço separadas para integrações internas.
7. Restringir manutenção do cadastro canônico de navios a perfis administrativos/planejamento.
8. Liberar os cabeçalhos de correlação necessários no CORS do Control Room.
9. Retornar `503 Service Unavailable` quando a consulta de work queues no Yard falhar, em vez de esconder a falha como lista vazia.

## Scheduler operacional implementado

1. Remover equipamentos, contêineres e coordenadas gerados com `Math.random()`.
2. Exigir `SchedulerPlanoOperacionalRequisicaoDto` com navio, equipamentos e posições reais.
3. Validar que as quantidades manifestadas correspondem às listas recebidas.
4. Validar janela de chegada e partida.
5. Considerar conflito somente entre navios do mesmo berço.
6. Preservar a duração da operação quando o slot for deslocado.
7. Persistir a agenda em `vessel_schedule`, evitando perda ao reiniciar o serviço.
8. Calcular o diagnóstico pela quantidade real de movimentos planejados, removendo a fórmula arbitrária `horas x 20`.
9. Restringir a API do scheduler a perfis autorizados.

## Cadastro canônico de navios implementado

1. Definir `servico-navio` como fonte de verdade dos dados comuns do navio.
2. Vincular `NavioSiderurgico` ao cadastro canônico por `navioCadastroId` único.
3. Resolver cadastro existente por ID ou IMO antes de criar a extensão siderúrgica.
4. Copiar nome, IMO, bandeira, armador e LOA do cadastro canônico, mantendo localmente apenas a projeção operacional.
5. Sincronizar periodicamente a projeção siderúrgica com o cadastro canônico.

## Monólito modular de Navio implementado

1. Criar o runtime `backend/cloudport-monolito-navio` para carregar `servico-navio` e `servico-navio-siderurgico` no mesmo processo Spring Boot.
2. Preservar os limites de pacote, entidades e repositórios dos dois módulos durante a transição.
3. Extrair `CadastroNavioPorta` e `NavioCanonico` para desacoplar a regra siderúrgica do transporte HTTP.
4. Manter `NavioCadastroCliente` como adaptador HTTP para o deployment isolado do serviço siderúrgico.
5. Criar `CadastroNavioLocalAdapter` para consultar `NavioServico` diretamente quando o runtime unificado estiver ativo.
6. Desativar a chamada HTTP interna `servico-navio-siderurgico -> servico-navio` no modo local por `cloudport.modulo.navio.integracao=local`.
7. Adicionar Dockerfile do runtime unificado e configuração para usar os schemas existentes em uma única conexão PostgreSQL.
8. Adicionar teste unitário da integração local por ID e IMO.
9. Incluir o novo runtime no workflow de compilação e testes do CloudPort.
10. Empacotar separadamente as migrações dos módulos em namespaces exclusivos do classpath.
11. Executar dois históricos Flyway independentes pelo runtime unificado, um para `cloudport_navio` e outro para `cloudport_siderurgico`.
12. Criar automaticamente os schemas e configurar o `search_path` da conexão para os dois módulos.
13. Centralizar JWT, roles, credencial interna, endpoints públicos e CORS em uma única configuração de segurança do runtime.
14. Liberar `X-Correlation-Id` e `X-CloudPort-Service-Key` no CORS unificado.
15. Incluir as migrações dos dois módulos na imagem Docker do runtime.
16. Validar por testes a separação dos históricos Flyway, o empacotamento das migrações, nomes seguros de schema e o CORS combinado.
17. Criar o reator `backend/cloudport-navio-modules` com os três projetos como módulos Maven do mesmo build.
18. Permitir que `servico-navio` e `servico-navio-siderurgico` gerem JARs de biblioteca no perfil `modulo-monolito`, preservando o empacotamento executável quando compilados isoladamente.
19. Fazer o runtime unificado depender dos artefatos Maven dos dois módulos e remover o `build-helper-maven-plugin` que adicionava fontes externas.
20. Atualizar a imagem Docker e o workflow para compilar os módulos pelo reator Maven.
21. Criar teste de inicialização completa com PostgreSQL 16 em Testcontainers.
22. Executar as migrações reais dos dois módulos antes da validação do `EntityManagerFactory`.
23. Validar a criação dos dois schemas, os históricos Flyway e consultas em todos os 13 repositórios JPA do runtime.
24. Publicar as migrações de `servico-navio` em `cloudport/migrations/navio` dentro do próprio artefato Maven.
25. Publicar as migrações de `servico-navio-siderurgico` em `cloudport/migrations/navio-siderurgico` dentro do próprio artefato Maven.
26. Remover do runtime a cópia direta de recursos a partir dos diretórios irmãos dos dois serviços.
27. Incorporar o frontend Angular do Control Room na imagem do runtime unificado.
28. Expor `GET /assets/configuracao.json` dinamicamente para configurar API e origens confiáveis sem reconstruir o frontend.
29. Criar Compose com perfis `monolito` e `legado` para corte, comparação e retorno controlado.
30. Usar no Compose uma única instância PostgreSQL com os schemas e históricos Flyway preservados.
31. Apontar a configuração local do portal para o Control Room servido pelo runtime unificado em `8086`.
32. Validar no CI a sintaxe do Compose e a construção completa da imagem Docker com backend e frontend.
33. Documentar a troca de perfil, a preservação do volume e os cuidados para evitar jobs duplicados.

## Documentação da migração para monólito modular implementada

1. Criar `README.md` na raiz como entrada principal da documentação do sistema.
2. Registrar o monólito modular como arquitetura alvo e impedir a criação indiscriminada de novos microsserviços internos.
3. Documentar o estado atual, o estado alvo, os limites de domínio, comunicação, persistência, segurança, observabilidade, build, deployment e rollback em `docs/arquitetura-monolito-modular.md`.
4. Adicionar guia de compilação, teste, configuração, banco e Docker em `backend/cloudport-monolito-navio/README.md`.
5. Atualizar o README do frontend para consumir uma única `baseApiUrl` e não conhecer hosts de módulos internos.
6. Marcar `servico-gate` como deployment legado em transição e remover referências de documentação inexistentes.
7. Atualizar os requisitos pendentes com o roteiro de incorporação de Yard, Gate, Rail, Autenticação e Visibilidade.

## Visibilidade operacional implementada

1. Remover mapeamentos Spring MVC duplicados de contêineres, alertas e navios que impediam a inicialização segura do serviço.
2. Consolidar rastreamento e histórico de contêineres em um único contrato e serviço.
3. Persistir eventos reais de entrada e saída no gate, armazenagem no pátio e movimento ferroviário.
4. Processar atualização de capacidade do pátio sem exigir `containerId`.
5. Preservar o status do navio quando o evento altera somente o berço.
6. Criar a projeção de status quando o evento de navio chega antes do cadastro na visibilidade.
7. Resolver alertas de atraso quando a chegada do navio for confirmada.
8. Substituir `System.out` e injeção por campo por logging estruturado e injeção por construtor nos fluxos alterados.
9. Remover totais, velocidades, previsões e tempos fictícios dos DTOs de navio, gate, pátio e rastreamento.
10. Calcular throughput do gate somente com eventos `ENTRADA_GATE` e `SAIDA_GATE`, pareando ciclos reais por contêiner.
11. Corrigir o contrato `estimadoParaida` para `estimadoParaSaida`, mantendo alias de leitura compatível.
12. Padronizar erros da API com `codigo`, `mensagem`, `detalhes`, `correlationId` e `timestamp`.
13. Exigir motivo para resolver alertas.
14. Externalizar configurações de banco, RabbitMQ, Redis, porta, emissor JWT e meta diária do gate.
15. Corrigir o emissor JWT padrão da visibilidade para o serviço de autenticação na porta `8080`.
16. Desabilitar Open Session in View no serviço de visibilidade.
17. Incluir Autenticação, Gate, Rail e Visibilidade na matriz de validação do backend, executando os testes de Visibilidade.

## Contratos de API implementados

```text
GET   /assets/configuracao.json
GET   /yard/patio/work-queues?visitaNavioId={id}
POST  /yard/patio/work-queues
PATCH /yard/patio/work-queues/{id}/ativar
PATCH /yard/patio/work-queues/{id}/desativar
PATCH /yard/patio/work-queues/{id}/pow
PATCH /yard/patio/work-queues/{id}/equipamento
PATCH /yard/patio/work-queues/{id}/ordens
GET   /yard/patio/work-queues/{id}/job-list
POST  /yard/patio/work-queues/{id}/dispatch
POST  /yard/patio/work-instructions/{id}/reset
POST  /yard/patio/work-instructions/{id}/cancelar
GET   /visitas-navio/{id}/integracao-patio/work-queues
POST  /api/scheduler/gerar-plano
GET   /api/v1/visibilidade/dashboard
GET   /api/v1/visibilidade/navios
GET   /api/v1/visibilidade/navios/{navioId}/detalhes
GET   /api/v1/visibilidade/patio/ocupacao
GET   /api/v1/visibilidade/gate/throughput
GET   /api/v1/visibilidade/alertas
POST  /api/v1/visibilidade/alertas/{id}/resolver
GET   /api/v1/visibilidade/conteiners/{containerId}/track
GET   /api/v1/visibilidade/conteiners/{containerId}/historico
GET   /api/v1/visibilidade/conteiners/buscar
```

## Testes implementados

1. Testes de contrato Angular para work queues, job list, dispatch, reset e cancelamento.
2. Testes de componente para filtros, totalização, expansão e ações operacionais.
3. Testes do scheduler com dados reais, quantidades inconsistentes, conflito no mesmo berço e simultaneidade em berços diferentes.
4. Teste do contrato de dispatch atualizado no componente.
5. Testes da porta local de cadastro canônico usada pelo runtime unificado de Navio.
6. Testes da configuração Flyway modular e da segurança CORS do runtime unificado.
7. Validação do build do runtime usando dependências Maven reais dos módulos, sem inclusão direta dos diretórios de fontes.
8. Teste de contexto com PostgreSQL/Testcontainers, `ddl-auto=validate`, migrações reais e exercício de todos os repositórios JPA.
9. Teste da origem classpath das migrações, garantindo que cada recurso seja fornecido pelo artefato do módulo responsável.
10. Teste unitário da configuração dinâmica entregue ao frontend incorporado.
11. Validação CI do Docker Compose e da imagem completa do runtime unificado.
12. Testes de persistência da localização atual e do histórico de movimentos de contêineres.
13. Testes dos listeners de gate, pátio, rail e navio, incluindo capacidade sem `containerId`.
14. Testes de preservação de status na atribuição de berço e criação da projeção de navio.
15. Testes do throughput real do gate e da ausência de tempo médio inventado sem ciclo completo.
16. Teste de inicialização conjunta dos controllers para impedir rotas ambíguas.

## Itens que não devem voltar como pendência principal

1. CRUD operacional básico de visita, item e plano de estiva.
2. Integração inicial Navio + Pátio.
3. Work queues, job list e ações básicas do Control Room.
4. Reconciliação automática agendada Pátio -> Navio.
5. Reserva em posição real livre do mapa do Yard.
6. Autenticação do Control Room e integração ao portal principal.
7. Credencial interna entre os serviços envolvidos.
8. Vínculo persistente entre work queue e work instruction.
9. Scheduler baseado em dados operacionais reais e agenda persistente.
10. Cadastro canônico como fonte dos dados comuns do navio.
11. Primeira execução conjunta de Navio e Navio Siderúrgico com chamada local ao cadastro canônico.
12. Execução das migrações dos dois schemas e configuração de segurança pelo runtime unificado.
13. Consumo de `servico-navio` e `servico-navio-siderurgico` como módulos Maven reais pelo runtime unificado.
14. Inicialização validada contra PostgreSQL real com os dois schemas e todos os repositórios JPA.
15. Migrações Flyway publicadas e carregadas a partir dos artefatos Maven dos respectivos módulos.
16. Imagem unificada com frontend incorporado, configuração dinâmica e Compose de transição entre perfis.
17. Definição e documentação do monólito modular como arquitetura alvo do CloudPort.
18. Rotas únicas e métricas baseadas em eventos reais no serviço de Visibilidade.

## Arquivos de execução consolidados e removidos de `docs/requisitos`

```text
docs/requisitos/execucao-automatica-cloudport-20260708-2036.md
docs/requisitos/execucao-automatica-cloudport-20260708-2139.md
docs/requisitos/execucao-automatica-cloudport-20260708-2238.md
docs/requisitos/execucao-automatica-cloudport-20260709-0630.md
docs/requisitos/execucao-automatica-cloudport-20260709-1030.md
docs/requisitos/execucao-automatica-cloudport-20260709-1038.md
docs/requisitos/execucao-automatica-cloudport-20260709-claudeport-fallback.md
docs/requisitos/execucao-automatica-cloudport-20260709-frontend-workqueues.md
```
