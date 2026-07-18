# Requisitos implementados do CloudPort

## Objetivo

Este documento registra o estado funcional e técnico já entregue no CloudPort. Ele deve ser atualizado sempre que uma funcionalidade deixa o backlog e passa a existir no código, nas migrações, nos contratos, nas telas ou na infraestrutura.

A lista de pendências comprovadas permanece em `docs/requisitos/requisito-tecnico.md`. Lacunas funcionais remanescentes ficam em `docs/requisitos/modulo-navios-back-front-gaps.md`.

## Arquitetura canônica entregue

O backend oficial é o monólito modular `backend/cloudport-runtime`, executado em um único processo Spring Boot.

### Módulos incorporados

1. Autenticação;
2. Carga Geral;
3. Gate;
4. Rail;
5. Visibilidade;
6. Yard;
7. Navio;
8. Navio Siderúrgico.

O módulo `backend/cloudport-contracts` concentra DTOs, enums e eventos compartilhados sem compartilhar entidades JPA ou repositories.

### Persistência

O runtime utiliza uma conexão PostgreSQL e oito schemas com ownership independente:

| Módulo | Schema |
| --- | --- |
| Autenticação | `cloudport_autenticacao` |
| Carga Geral | `cloudport_carga_geral` |
| Gate | `cloudport_gate` |
| Rail | `cloudport_rail` |
| Visibilidade | `cloudport_visibilidade` |
| Yard | `cloudport_yard` |
| Navio | `cloudport_navio` |
| Navio Siderúrgico | `cloudport_siderurgico` |

Cada módulo mantém seu próprio `flyway_schema_history`. O runtime extrai as migrações para namespaces exclusivos antes de criar o `EntityManagerFactory`.

### Comunicação interna

Foram substituídas por portas e adaptadores locais as integrações internas entre:

- Navio Siderúrgico e Navio;
- Navio Siderúrgico e Yard;
- Navio e Yard;
- Gate e Autenticação;
- Gate e Yard;
- Gate e Navio;
- Rail e Navio.

TOS, OCR, EDI, RabbitMQ, Redis, storage e webhooks permanecem integrações externas de borda.

### Infraestrutura transversal

O runtime centraliza:

- segurança JWT;
- CORS;
- OpenAPI;
- conexão PostgreSQL;
- Flyway;
- cache local e Redis;
- configuração de jobs e consumidores;
- correlação e tracing;
- tratamento de erros compartilhado nos fluxos já migrados.

## Segurança e identidade

### Autenticação e autorização

Foram implementados:

- login e emissão de JWT;
- usuários, papéis e permissões;
- segredo JWT externo obrigatório com pelo menos 32 bytes;
- rejeição de credenciais funcionais padrão;
- clientes públicos externos configuráveis;
- comparação segura de segredos;
- autorização por operação nos comandos de estiva e pátio;
- separação entre consulta, administração e execução;
- imutabilidade de planos em estados finais;
- configuração dinâmica da navegação por perfil.

### Sessão do portal

O cliente HTTP e a árvore React foram integrados para:

- invalidar a sessão em qualquer resposta `401`;
- remover a sessão persistida;
- impedir novas chamadas protegidas sem token;
- redirecionar para o login;
- aceitar somente caminhos de retorno internos e seguros;
- preservar chamadas explicitamente públicas.

### API pública

O line-up público de navios expõe somente dados sanitizados. Identificadores internos, observações administrativas e dados operacionais restritos não fazem parte do contrato.

## Gate completo

### Configuração e referências

Foram entregues:

- facilities;
- múltiplos Gates;
- pistas;
- consoles;
- áreas de troca;
- estágios configuráveis;
- transições entre estágios;
- business tasks;
- regras de acesso;
- transportadoras, motoristas e veículos;
- motivos operacionais;
- impressoras e documentos.

### Documentos e ordens

O Gate suporta:

- bookings;
- Bill of Lading;
- Equipment Delivery Order;
- Equipment Receive Order;
- Import Delivery Order;
- pré-avisos;
- anexos e fotografias;
- tickets e comprovantes;
- emissão e reimpressão de EIR.

### Agendamentos e visitas

Foram implementados:

- appointments com janelas e capacidade;
- consumo transacional da capacidade;
- truck visits com múltiplas transações;
- histórico de estágios;
- inspeções;
- trouble transactions;
- regras de bloqueio e permissão;
- transferências entre instalações;
- relatórios persistidos por período, operação e transportadora;
- indicadores de pontualidade, no-show, ocupação, abandono e turnaround.

### Gate visual

A central visual apresenta:

- quadro de pistas;
- filas por Gate e estágio;
- calendário de agendamentos;
- ocupação versus capacidade;
- jornada do veículo;
- estados de OCR, balança, inspeção e liberação;
- documentos, imagens e avarias;
- transações problemáticas;
- cronômetro e classificação de SLA;
- impressão e reimpressão de EIR.

### Console do operador

As telas usam os contratos reais de painel e eventos, consolidando entrada, atendimento e saída sem reutilizar dados genéricos de agendamento.

### Controle de pessoas

Foi implementado o controle de entrada e saída de pessoas com:

- cadastro operacional;
- situação atual `DENTRO` ou `FORA`;
- presença atual;
- histórico auditável;
- ponto de acesso;
- operador;
- origem da ação;
- correlation ID;
- tempo de permanência;
- normalização documental;
- bloqueio de entrada duplicada e saída sem entrada aberta;
- APIs protegidas e tela React.

O tratamento concorrente final continua registrado como pendência `ERR10`.

### Fluxos diretos

Foram entregues dois fluxos especiais:

1. carga autopropelida descarregada do navio pode sair diretamente pelo Gate após validação documental, aduaneira, operacional e do condutor, sem visita fictícia de caminhão;
2. contêiner recepcionado no Gate pode seguir diretamente ao cais e ser confirmado no plano de estiva, sem posição ou ordem no pátio.

Os dois fluxos registram auditoria, evitam duplicidade e preservam a sequência operacional.

## Carga Geral, projeto e break-bulk

O módulo `servico-carga-geral` foi incorporado ao runtime canônico.

### Domínio

Foram implementados:

- Bill of Lading;
- itens do conhecimento;
- cargo lots;
- carga solta;
- carga de projeto;
- break-bulk;
- commodities;
- tipos de embalagem;
- tipos de produto;
- códigos de armazenagem;
- códigos de manuseio;
- mercadorias perigosas;
- número UN e classe IMDG;
- faixas de temperatura;
- avarias de carga;
- quantidades, volume e peso;
- saldo previsto e saldo em estoque;
- recebimento;
- carga e descarga parcial;
- transferência;
- consolidação e desconsolidação;
- vínculos com veículo, visita de navio, armazém e cliente.

### Regras

O domínio aplica:

- saldo não negativo;
- bloqueio pessimista nas movimentações de estoque;
- número UN e classe IMDG obrigatórios para mercadoria perigosa;
- temperatura mínima não superior à máxima;
- unicidade de documentos, itens, lotes e referências;
- rastreabilidade das movimentações.

O tratamento funcional das colisões concorrentes de unicidade permanece registrado como `ERR40`.

### Interface

Foram adicionados dashboard, console operacional, rotas, navegação, contratos e testes.

## Inventário canônico

Foi criado um domínio unificado de unidade e equipamento no Yard.

### Unidade e equipamento

O inventário cobre:

- ciclo de vida completo;
- contêiner;
- chassi;
- carreta;
- acessório;
- tipo ISO;
- dimensões e capacidade;
- prefixos;
- equivalências de tipo;
- propriedade e operador;
- montagem e desmontagem;
- histórico de atributos.

### Controle operacional

Também foram implementados:

- lacres;
- documentos;
- avarias;
- componentes e condições;
- manutenção e reparo;
- holds e permissions;
- controle reefer;
- inventário físico;
- divergências;
- sincronização do inventário legado de `conteiner_patio`.

### API e interface

O contrato canônico está disponível em `/yard/inventario/canonico`. A interface possui indicadores, filtros, cadastro, inspector completo e ações rápidas integradas ao `OperationalDataGrid`.

## Pátio operacional

### Mapa e vistas

Foram entregues:

- mapa georreferenciado com Google Maps;
- imagem de satélite;
- polígonos de blocos e pilhas;
- fallback sem chave do Google Maps;
- parâmetros de coordenadas, dimensão, rotação, zoom e tipo de mapa por ambiente;
- vista de bloco;
- seção lateral;
- scan;
- microvisão da pilha;
- sincronização entre mapa, grade e detalhe.

### Camadas e workspaces

A interface possui:

- ocupação;
- dwell time;
- reefers;
- bloqueios;
- interdições;
- posições reservadas;
- pilhas cheias;
- notas operacionais;
- filtros;
- workspaces salvos no navegador.

### Movimentação e restrições

Foram implementados:

- drag-and-drop de contêiner;
- alternativa acessível por seleção;
- pré-visualização de origem e destino;
- validação do destino no backend;
- bloqueio de posição inexistente, ocupada, proibida ou reservada;
- edição motivada de bloqueio, interdição, permissão e nota;
- auditoria das ações operacionais.

### Work queues e work instructions

O módulo possui:

- work queues persistentes;
- associação de ordens;
- POW;
- equipamento;
- job list;
- dispatch;
- suspensão e retomada;
- prioridade operacional;
- bloqueio;
- conclusão;
- reset;
- cancelamento;
- motivo obrigatório;
- limite real de dispatch;
- auditoria;
- autorização separada para consulta, administração e operação.

### Reshuffling

O reshuffling utiliza o mapa real:

- identifica o bloqueador pela posição física;
- seleciona posição persistida e compatível;
- valida ocupação, reserva, peso, tipo, capacidade e apoio;
- reserva o destino com lock pessimista;
- cria a ordem na mesma transação;
- usa chave idempotente;
- recalcula destino após conflito;
- libera reserva na conclusão ou no cancelamento.

O agendamento noturno é executado somente quando os jobs do runtime canônico estão explicitamente habilitados.

### Planejamento de recebimento

O endpoint `POST /yard/patio/planejamento-recebimento` agrupa contêineres por:

- janela de quatro horas;
- categoria;
- armador;
- visita de saída;
- destino;
- comprimento ISO;
- tipo de equipamento;
- estado da carga;
- reefer;
- classe IMO;
- faixa de peso;
- prioridade operacional.

O resultado apresenta quantidade, TEU, peso e alertas de dados críticos ausentes.

### Otimização de posição

O autoplanejamento utiliza atribuição global por custo mínimo com algoritmo Húngaro.

Restrições obrigatórias:

- posição livre;
- bloqueio e interdição;
- área permitida;
- reserva ativa;
- tipo de carga;
- peso;
- altura e capacidade;
- apoio físico da camada;
- tomada reefer;
- isolamento de perigosos.

Custos considerados:

- risco de rehandle;
- distância até equipamento operacional;
- mistura de destinos e tipos;
- abertura de nova pilha;
- camada;
- concentração no bloco.

O processamento em rodadas impede planejamento de camadas superiores sem apoio inferior.

### Reefers, rotas e allocations

Foram implementados:

- telemetria persistida de temperatura;
- faixa permitida;
- alimentação elétrica;
- instante da leitura;
- alarmes `NORMAL`, `ATENCAO` e `CRITICO`;
- detecção de leitura atrasada, equipamento desligado e temperatura fora da faixa;
- rotas no Google Maps entre a posição atual e o destino da work instruction;
- editor gráfico de allocations;
- posições elegíveis;
- simulação;
- confirmação motivada;
- validação concorrente do destino.

## Ferrovia

### Operação básica

Foram implementados:

- visitas ferroviárias;
- manifestos;
- locomotivas e vagões;
- contêineres por vagão;
- ordens de carga e descarga;
- lista de trabalho;
- filtros e métricas;
- início e conclusão de movimentações;
- sincronização do item do manifesto;
- fase `CONCLUIDO`;
- partida somente após conclusão integral.

### Line-up vertical

A tela interna apresenta:

- linhas ferroviárias em colunas;
- tempo na vertical;
- recepção, operação e expedição;
- conflitos de ocupação;
- indicadores e detalhamento.

### Composição gráfica

A ferrovia visual possui:

- locomotiva e vagões em sequência;
- contêineres associados a cada vagão;
- progresso individual;
- ocupação das linhas;
- cronograma de chegada, operação e partida;
- indicação de vagão bloqueado ou incompatível;
- drag-and-drop e seletor acessível para simulação do replanejamento.

A persistência do replanejamento visual por vagão ainda é uma lacuna funcional documentada.

### Locomotiva para navio

A locomotiva isolada é tratada como a própria visita ferroviária:

- tipo `LOCOMOTIVA_ISOLADA`;
- identidade compartilhada entre visita e operação de embarque;
- proibição de vagões e contêineres na mesma visita;
- entrega de custódia;
- planejamento no navio;
- checklist de freio, baterias, combustível, calços e amarração;
- modalidade Ro-Ro ou Lo-Lo;
- confirmação do embarque;
- encerramento da visita ferroviária.

## Navio e Vessel Planner

### Cadastro e identidade canônica

Foram implementados:

- cadastro canônico de navios;
- visitas operacionais;
- versionamento da fonte;
- porta de consulta canônica;
- validação de navio, visita e viagem;
- bloqueio de comandos quando a fonte foi alterada;
- vínculo dos planejadores de contêineres e bobinas à mesma identidade.

### BAPLIE e atributos operacionais

O processamento suporta perfis BAPLIE D.95B/SMDG e D.13B/SMDG31 e preserva:

- identidade do navio e viagem;
- operação;
- cheio ou vazio;
- peso bruto;
- VGM, origem e status;
- parâmetros reefer;
- classe IMO e número ONU;
- embalagem e segregação;
- dimensões OOG;
- posição de seis ou sete dígitos;
- segmentos originais para auditoria.

Pesos em KGM, TNE, LBR e GRM são normalizados para quilogramas.

### Geometria real

O Vessel Planner utiliza perfil geométrico aprovado e versionado com:

- bays;
- rows;
- tiers;
- hatch covers;
- slots restritos;
- tomadas reefer;
- comprimentos admissíveis;
- limites por slot e pilha;
- condição de carregamento;
- dados hidrostáticos.

Criação, alocação, autoestivagem e aprovação são bloqueadas quando o perfil está ausente, inválido ou incompatível.

### Estabilidade e resistência

Foram implementados cálculos versionados para:

- peso total;
- LCG;
- TCG;
- VCG;
- GM;
- calado;
- trim;
- banda;
- força cortante;
- momento fletor;
- limites por seção.

Entradas, versões, memória de cálculo, resultados e aprovação são persistidos. Mudanças na distribuição invalidam a aprovação.

### Interface gráfica completa

O Vessel Planner apresenta:

- profile view;
- top view;
- section view;
- tier view;
- vistas sincronizadas;
- modo multivisão;
- inspector lateral por slot;
- drag-and-drop da load list para o navio;
- movimentação entre slots;
- legendas por POD, peso, IMO, reefer e operador;
- tampas de porão;
- peso acumulado por stack;
- limites e alertas no slot;
- segregação IMDG;
- restow;
- sequência visual de guindastes;
- overlays de estabilidade, lashing e força estrutural.

O overlay de lashing é indicativo e não substitui cálculo certificado.

### Line-up de navios

O line-up operacional possui:

- ETA, ETB e ETD;
- berço, armador e navio;
- fases simuladas;
- relógio operacional;
- execução, pausa, avanço e retrocesso;
- progresso;
- atrasos;
- conflitos por sobreposição.

A visão interna vertical posiciona berços em colunas e o tempo de cima para baixo.

O line-up público está disponível em `/line-up`, com `GET /public/line-up-navios?dias=30`, acesso anônimo, filtros, cache de um minuto e contrato sanitizado.

## Navio Siderúrgico

### Planejamento de bobinas

Foram implementados:

- planos persistidos por navio e visita;
- manifesto de bobinas;
- porões e setores de tank top;
- posicionamento;
- consulta de tank top;
- empilhamento;
- estabilidade;
- securing;
- relatório;
- validação completa;
- aprovação explícita.

### Evidências de segurança

A aprovação exige:

- dimensões reais de apoio;
- geometria do porão;
- limites de camadas;
- espaçamento;
- dunnage;
- calços;
- sequência de descarga;
- materiais certificados;
- pontos de amarração;
- capacidade nominal;
- carga de trabalho segura;
- regras e versões de especificação;
- resultado e responsável.

Valores sintéticos de dunnage, ângulo, lashing, hidrostática ou GM não são usados para aprovar o plano.

## Control Room e equipamentos

Foram implementados:

- visão operacional dos equipamentos;
- status e posição;
- conectividade;
- VMT;
- work instruction atual;
- histórico de telemetria;
- atualização por SSE;
- detecção de telemetria atrasada;
- heartbeat ausente;
- falha de dispositivo;
- indisponibilidade;
- reconhecimento e resolução de alarmes;
- registro de indisponibilidades;
- comandos remotos;
- polling autenticado pelo dispositivo;
- envio, execução e confirmação;
- firmware, protocolo, endereço e sequência.

## Visibilidade e alertas

### Dashboard e eventos

Foram entregues:

- projeções operacionais;
- histórico;
- alertas;
- atualização por eventos;
- reconciliação periódica controlada;
- idempotência por identificador de mensagem;
- canais SSE e WebSocket versionados nos fluxos já migrados;
- envelope de evento;
- correlation ID e traceparent.

### Central global

A central global possui:

- indicador no cabeçalho;
- contagem de alertas ativos ou não reconhecidos;
- painel lateral;
- filtros por status e severidade;
- priorização visual;
- reconhecimento;
- resolução;
- navegação ao módulo relacionado;
- página `/home/alertas`;
- consulta paginada;
- resumo agregado por severidade.

## EDI e processamento assíncrono

Foram implementados:

- processamento persistente;
- idempotência por identificadores do intercâmbio e mensagem;
- retentativa assíncrona;
- fila de quarentena;
- outbox para publicação externa;
- limitação de tentativas;
- auditoria de rejeição e reprocessamento;
- worker habilitado somente por propriedade explícita;
- jobs do runtime condicionados por `CLOUDPORT_JOBS_ENABLED=true`;
- desativação padrão nos serviços standalone;
- lock e reivindicação para impedir processamento concorrente.

A reconciliação de barcode utiliza lock distribuído, lease, token de reivindicação e chave idempotente.

## Billing e CAP

Foram implementados:

- tarifas por operação e vigência;
- cobranças;
- geração idempotente para atendimentos concluídos;
- faturas;
- itens de fatura;
- consolidação de cobranças pendentes;
- pagamentos;
- quitação automática;
- isolamento por transportadora com dados do JWT;
- resumo CAP;
- telas, rotas e navegação.

As disputas concorrentes na geração de fatura e no registro de pagamentos permanecem registradas como `ERR20`.

## Interface compartilhada

### OperationalDataGrid

O componente compartilhado substituiu as tabelas genéricas e oferece:

- busca rápida sem diferenciar acentos;
- filtros combináveis por coluna;
- operadores contém, igual, começa com e vazio;
- ordenação de texto, número e data;
- paginação de 10, 25 ou 50 registros;
- suporte opcional a paginação, filtro e ordenação no backend;
- ocultação, exibição e reordenação de colunas;
- congelamento da primeira coluna visível;
- visões nomeadas persistidas;
- seleção múltipla;
- ações em lote;
- inspector lateral;
- navegação por teclado;
- `aria-sort`;
- exportação CSV;
- neutralização de fórmulas em CSV;
- exportação Excel em SpreadsheetML;
- exportação da seleção;
- inferência de todos os campos retornados, sem limite de oito colunas.

### Ajuda contextual

Todas as páginas que utilizam `PageHeader` recebem:

- botão Ajuda;
- painel lateral responsivo;
- conteúdo por rota e módulo;
- finalidade;
- fluxo recomendado;
- campos principais;
- permissões;
- estados;
- bloqueios;
- exemplo operacional;
- pesquisa sem diferenciação de acentos;
- atalhos `F1`, `Shift + ?` e `Esc`.

## Deploy e operação

### Backend no EasyPanel

Foi criado `backend/Dockerfile` para o contexto `/backend` com:

- instalação do parent Maven;
- inclusão de `cloudport-contracts`;
- inclusão dos oito módulos;
- empacotamento do runtime;
- diretório persistente de documentos;
- health check em `/actuator/health/readiness`;
- porta `8080`.

O Dockerfile executado pela raiz continua em `backend/cloudport-runtime/Dockerfile`.

### Frontend no EasyPanel

Foi criado `frontend/Dockerfile` multi-stage com:

- Node.js 22 para build;
- Nginx para execução;
- publicação de `dist/cloudport`;
- porta `80`;
- health check em `/health`;
- fallback de SPA;
- cache de assets;
- compressão gzip;
- `.dockerignore` próprio.

### Corte e rollback

Foram documentados:

- invariantes de execução única;
- variáveis de escrita, jobs e consumidores;
- oito schemas e históricos;
- build Maven e Docker;
- contextos do EasyPanel;
- smoke funcional completo;
- critérios de aprovação;
- rollback intermediário;
- rollback para serviços isolados;
- compatibilidade Flyway;
- condições de aborto.

## Referências de entregas recentes

| PR | Entrega |
| --- | --- |
| #393 | Correção do backend no EasyPanel |
| #392 | Imagem do frontend para EasyPanel |
| #386 | Operação completa do Gate |
| #385 | Carga Geral e break-bulk |
| #384 | Reefers, rotas e allocations no Yard |
| #383 | Inventory Management canônico |
| #382 | Gate visual operacional |
| #381 | Vessel Planner gráfico completo |
| #379 | Pátio mais operacional |
| #378 | Control Room, telemetria e equipamentos |
| #376 | Ferrovia visual e planejamento de vagões |
| #375 | Exportação Excel e grade operacional completa |
| #373 | Ajuda contextual |
| #371 | Billing e CAP |
| #370 | Central global de alertas |
| #368 | Line-up ferroviário vertical |
| #365 | Line-up interno vertical de navios |
| #363 | Locomotiva como visita ferroviária |
| #360 | Line-up público de navios |
| #357 | Controle de entrada e saída de pessoas |
| #353 | Conclusão e partida da visita ferroviária |
| #345 | Embarque direto do Gate para o navio |
| #343 | Pátio no Google Maps |
| #340 | Otimização global de posições do Yard |
| #339 | Agrupamento para recebimento no pátio |
| #338 | Saída direta de carga autopropelida |
| #318 | Estabilidade operacional versionada |
| #316 | Identidade canônica dos planejadores |
| #314 | Geometria real do navio |
| #303 | Validação completa da estiva de bobinas |
| #302 | Atributos operacionais do BAPLIE |

## Pendências não marcadas como implementadas

Este documento não encerra automaticamente itens do backlog. Permanecem válidos, entre outros, os requisitos atuais `ERR10`, `ERR20`, `ERR30`, `ERR40`, `SEC70`, `SEC80` e `SEC90`, conforme `docs/requisitos/requisito-tecnico.md`.