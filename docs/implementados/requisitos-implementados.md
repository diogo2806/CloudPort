# Requisitos implementados - CloudPort

## Instrucoes obrigatorias para agentes de IA

Esta pasta deve manter um unico arquivo: `docs/implementados/requisitos-implementados.md`.

Nao criar outros documentos, arquivos de evidencia, logs, historicos ou rascunhos nesta pasta. Toda entrega deve sair de `docs/requisitos/modulo-navios-back-front-gaps.md` e ser registrada aqui sem duplicacao.

## Modulo Navio implementado

1. Criar visita de navio.
2. Criar item operacional de embarque, descarga e restow.
3. Criar plano de estiva por visita.
4. Criar eventos e resumo operacional da visita.
5. Criar endpoints basicos `/visitas-navio`.
6. Criar endpoints basicos de integracao em `/visitas-navio/{id}/integracao-patio`.
7. Adicionar campos de integracao em `ItemOperacaoNavio`.
8. Expor relatorio operacional integrado basico.

## Integracao Navio + Patio implementada

1. Criar reserva de patio vinculada ao item de navio.
2. Adicionar campos de visita, item e plano em `OrdemTrabalhoPatio`.
3. Criar ordem real no `servico-yard` por `/yard/patio/ordens/navio`.
4. Impedir ordem ativa duplicada por `visitaNavioId + itemOperacaoNavioId`.
5. Expor filas e ordens sem cobertura por visita no Yard e no modulo de navio.
6. Permitir sincronizacao manual de status.
7. Permitir gerar reservas e ordens de patio.
8. Permitir replanejamento inicial.
9. Permitir alterar prioridade, suspender e retomar ordens.
10. Reconciliar automaticamente visitas ativas por job agendado.
11. Atualizar item para `EM_MOVIMENTO`, `OPERADO`, `BLOQUEADO` ou `CANCELADO` conforme a ordem real.
12. Preencher posicao real, consumir reserva ao concluir e cancelar reserva ao cancelar ordem.
13. Registrar eventos somente quando a reconciliacao alterar dados, evitando historico vazio repetitivo.

## Control Room implementado

1. Criar painel Angular Navio + Patio com filtros, movimentos iminentes, filas, reservas, ordens, alertas e excecoes.
2. Permitir gerar reservas, gerar ordens, sincronizar, replanejar, priorizar, suspender e retomar ordens.
3. Carregar e renderizar work queues persistentes com job list expansivel.
4. Permitir ativar/desativar fila, editar POW/pool/equipamento, executar dispatch, resetar e cancelar work instruction.
5. Exibir loading por acao e feedback de sucesso/erro.
6. Integrar o modulo ao portal principal pela rota autenticada `/home/navio/control-room`.
7. Implementar SSO entre o portal e o modulo por `postMessage` restrito a origens configuradas.
8. Implementar login proprio como fallback e limitar acesso aos perfis operacionais.
9. Enviar JWT, usuario autenticado, origem da acao e `X-Correlation-Id` nas chamadas operacionais.
10. Exibir `codigo`, `mensagem`, `detalhes` e `correlationId` quando retornados pelo backend.
11. Executar as consultas do snapshot em paralelo, aplicar o resultado de forma atomica e impedir atualizacoes sobrepostas.

## Work queues implementadas

1. Listar, criar, ativar e desativar work queue.
2. Associar POW, pool operacional e equipamento.
3. Expor job list e executar dispatch.
4. Resetar e cancelar work instruction.
5. Expor work queues pelo modulo de navio.
6. Persistir `workQueueId` em `OrdemTrabalhoPatio`.
7. Atualizar explicitamente a job list por `PATCH /yard/patio/work-queues/{id}/ordens`.
8. Vincular automaticamente uma ordem apenas quando existir uma unica fila compativel, evitando associacao ambigua.
9. Remover a comparacao incorreta entre camada de destino e bloco/zona.
10. Honrar `limiteOrdens` no dispatch.
11. Padronizar a resposta com `workQueueId`, `totalOrdensDespachadas`, `totalOrdensIgnoradas`, `ordens` e `mensagem`.
12. Persistir auditoria de criacao, ativacao, desativacao, POW/pool, equipamento, vinculo, dispatch, reset e cancelamento.
13. Restringir operacoes a perfis autorizados e ao servico interno de navios.

## Reserva contra mapa real implementada

1. Consultar `GET /yard/patio/posicoes` antes de reservar.
2. Selecionar somente posicao real com linha, coluna e camada.
3. Recusar mapa vazio, posicao inexistente, posicao ocupada e posicao com reserva ativa.
4. Remover a geracao de identificadores artificiais como `V{visita}-D-{sequencia}`.
5. Armazenar identificador e coordenadas reais na reserva.
6. Garantir que a reserva gerada contenha os dados exigidos para criar a ordem real no Yard.

## Autenticacao e seguranca implementadas

1. Preservar a senha digitada sem remover caracteres antes do login.
2. Nao armazenar senha no `localStorage`.
3. Armazenar no portal somente os dados seguros da sessao.
4. Autenticar chamadas entre `servico-navio-siderurgico`, `servico-yard` e `servico-navio` com `X-CloudPort-Service-Key`.
5. Usar comparacao constante da credencial interna.
6. Aplicar roles de servico separadas para integracoes internas.
7. Restringir manutencao do cadastro canonico de navios a perfis administrativos/planejamento.
8. Liberar os cabecalhos de correlacao necessarios no CORS do Control Room.
9. Retornar `503 Service Unavailable` quando a consulta de work queues no Yard falhar, em vez de esconder a falha como lista vazia.

## Scheduler operacional implementado

1. Remover equipamentos, conteineres e coordenadas gerados com `Math.random()`.
2. Exigir `SchedulerPlanoOperacionalRequisicaoDto` com navio, equipamentos e posicoes reais.
3. Validar que as quantidades manifestadas correspondem as listas recebidas.
4. Validar janela de chegada e partida.
5. Considerar conflito somente entre navios do mesmo berco.
6. Preservar a duracao da operacao quando o slot for deslocado.
7. Persistir a agenda em `vessel_schedule`, evitando perda ao reiniciar o servico.
8. Calcular o diagnostico pela quantidade real de movimentos planejados, removendo a formula arbitraria `horas x 20`.
9. Restringir a API do scheduler a perfis autorizados.

## Cadastro canonico de navios implementado

1. Definir `servico-navio` como fonte de verdade dos dados comuns do navio.
2. Vincular `NavioSiderurgico` ao cadastro canonico por `navioCadastroId` unico.
3. Resolver cadastro existente por ID ou IMO antes de criar a extensao siderurgica.
4. Copiar nome, IMO, bandeira, armador e LOA do cadastro canonico, mantendo localmente apenas a projecao operacional.
5. Sincronizar periodicamente a projecao siderurgica com o cadastro canonico.

## Monolito modular de Navio implementado

1. Criar o runtime `backend/cloudport-monolito-navio` para carregar `servico-navio` e `servico-navio-siderurgico` no mesmo processo Spring Boot.
2. Preservar os limites de pacote, entidades e repositorios dos dois modulos durante a transicao.
3. Extrair `CadastroNavioPorta` e `NavioCanonico` para desacoplar a regra siderurgica do transporte HTTP.
4. Manter `NavioCadastroCliente` como adaptador HTTP para o deployment isolado do servico siderurgico.
5. Criar `CadastroNavioLocalAdapter` para consultar `NavioServico` diretamente quando o runtime unificado estiver ativo.
6. Desativar a chamada HTTP interna `servico-navio-siderurgico -> servico-navio` no modo local por `cloudport.modulo.navio.integracao=local`.
7. Adicionar Dockerfile do runtime unificado e configuracao para usar os schemas existentes em uma unica conexao PostgreSQL.
8. Adicionar teste unitario da integracao local por ID e IMO.
9. Incluir o novo runtime no workflow de compilacao e testes do CloudPort.
10. Empacotar separadamente as migracoes dos modulos em namespaces exclusivos do classpath.
11. Executar dois historicos Flyway independentes pelo runtime unificado, um para `cloudport_navio` e outro para `cloudport_siderurgico`.
12. Criar automaticamente os schemas e configurar o `search_path` da conexao para os dois modulos.
13. Centralizar JWT, roles, credencial interna, endpoints publicos e CORS em uma unica configuracao de seguranca do runtime.
14. Liberar `X-Correlation-Id` e `X-CloudPort-Service-Key` no CORS unificado.
15. Incluir as migracoes dos dois modulos na imagem Docker do runtime.
16. Validar por testes a separacao dos historicos Flyway, o empacotamento das migracoes, nomes seguros de schema e o CORS combinado.
17. Criar o reator `backend/cloudport-navio-modules` com os tres projetos como modulos Maven do mesmo build.
18. Permitir que `servico-navio` e `servico-navio-siderurgico` gerem JARs de biblioteca no perfil `modulo-monolito`, preservando o empacotamento executavel quando compilados isoladamente.
19. Fazer o runtime unificado depender dos artefatos Maven dos dois modulos e remover o `build-helper-maven-plugin` que adicionava fontes externas.
20. Atualizar a imagem Docker e o workflow para compilar os modulos pelo reator Maven.
21. Criar teste de inicializacao completa com PostgreSQL 16 em Testcontainers.
22. Executar as migracoes reais dos dois modulos antes da validacao do `EntityManagerFactory`.
23. Validar a criacao dos dois schemas, os historicos Flyway e consultas em todos os 13 repositorios JPA do runtime.
24. Publicar as migracoes de `servico-navio` em `cloudport/migrations/navio` dentro do proprio artefato Maven.
25. Publicar as migracoes de `servico-navio-siderurgico` em `cloudport/migrations/navio-siderurgico` dentro do proprio artefato Maven.
26. Remover do runtime a copia direta de recursos a partir dos diretorios irmaos dos dois servicos.
27. Incorporar o frontend Angular do Control Room na imagem do runtime unificado.
28. Expor `GET /assets/configuracao.json` dinamicamente para configurar API e origens confiaveis sem reconstruir o frontend.
29. Criar Compose com perfis `monolito` e `legado` para corte, comparacao e retorno controlado.
30. Usar no Compose uma unica instancia PostgreSQL com os schemas e historicos Flyway preservados.
31. Apontar a configuracao local do portal para o Control Room servido pelo runtime unificado em `8086`.
32. Validar no CI a sintaxe do Compose e a construcao completa da imagem Docker com backend e frontend.
33. Documentar a troca de perfil, a preservacao do volume e os cuidados para evitar jobs duplicados.

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
```

## Testes implementados

1. Testes de contrato Angular para work queues, job list, dispatch, reset e cancelamento.
2. Testes de componente para filtros, totalizacao, expansao e acoes operacionais.
3. Testes do scheduler com dados reais, quantidades inconsistentes, conflito no mesmo berco e simultaneidade em bercos diferentes.
4. Teste do contrato de dispatch atualizado no componente.
5. Testes da porta local de cadastro canonico usada pelo runtime unificado de Navio.
6. Testes da configuracao Flyway modular e da seguranca CORS do runtime unificado.
7. Validacao do build do runtime usando dependencias Maven reais dos modulos, sem inclusao direta dos diretorios de fontes.
8. Teste de contexto com PostgreSQL/Testcontainers, `ddl-auto=validate`, migracoes reais e exercicio de todos os repositorios JPA.
9. Teste da origem classpath das migracoes, garantindo que cada recurso seja fornecido pelo artefato do modulo responsavel.
10. Teste unitario da configuracao dinamica entregue ao frontend incorporado.
11. Validacao CI do Docker Compose e da imagem completa do runtime unificado.

## Itens que nao devem voltar como pendencia principal

1. CRUD operacional basico de visita, item e plano de estiva.
2. Integracao inicial Navio + Patio.
3. Work queues, job list e acoes basicas do Control Room.
4. Reconciliacao automatica agendada Patio -> Navio.
5. Reserva em posicao real livre do mapa do Yard.
6. Autenticacao do Control Room e integracao ao portal principal.
7. Credencial interna entre os servicos envolvidos.
8. Vinculo persistente entre work queue e work instruction.
9. Scheduler baseado em dados operacionais reais e agenda persistente.
10. Cadastro canonico como fonte dos dados comuns do navio.
11. Primeira execucao conjunta de Navio e Navio Siderurgico com chamada local ao cadastro canonico.
12. Execucao das migracoes dos dois schemas e configuracao de seguranca pelo runtime unificado.
13. Consumo de `servico-navio` e `servico-navio-siderurgico` como modulos Maven reais pelo runtime unificado.
14. Inicializacao validada contra PostgreSQL real com os dois schemas e todos os repositorios JPA.
15. Migracoes Flyway publicadas e carregadas a partir dos artefatos Maven dos respectivos modulos.
16. Imagem unificada com frontend incorporado, configuracao dinamica e Compose de transicao entre perfis.

## Arquivos de execucao consolidados e removidos de `docs/requisitos`

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
