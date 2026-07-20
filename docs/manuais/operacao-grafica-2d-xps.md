# Operação gráfica 2D equivalente ao XPS

## Finalidade

O workspace gráfico 2D integra planejamento e execução de Navio, cais, pátio, ferrovia, work queues e equipamentos de movimentação. A mesma unidade, work instruction, origem, destino e CHE permanecem correlacionados durante seleção, simulação, confirmação e acompanhamento.

O processo cobre os requisitos BUS1630 a BUS1730 e complementa a seleção compartilhada, navegação hierárquica, timeframes e estados operacionais dos BUS1590 a BUS1620.

## Acesso

As ferramentas são exibidas dentro do Vessel Planner e das vistas operacionais do pátio.

Permissões de consulta:

- `ADMIN_PORTO`;
- `PLANEJADOR`;
- `OPERADOR_PATIO`;
- `OPERADOR_NAVIO`;
- `OPERADOR_FERROVIA`.

A confirmação continua submetida às regras, locks, versões e permissões do domínio afetado. Somente `ADMIN_PORTO` pode publicar um workspace como padrão administrativo.

## Fluxo operacional completo

1. Abra o Vessel Planner ou o mapa operacional do pátio.
2. Selecione a aba da ferramenta desejada.
3. Selecione unidades, posições, filas, equipamentos, vagões ou elementos físicos.
4. Configure padrão, sentido, filtros, alcance, layout, geometria ou rota.
5. Revise a proposta, a sequência numerada, os conflitos, os bloqueios, o ETA e a produtividade estimada.
6. Confirme a proposta.
7. O frontend envia um `commandId` único ao endpoint `/api/operacao-2d/comandos`.
8. O backend valida o tipo e o payload, impede processamento duplicado e persiste o snapshot do comando.
9. Workspaces são salvos em `/api/operacao-2d/workspaces`, sempre como uma nova versão.
10. Em caso de erro, ajuste a proposta e envie um novo comando. O comando anterior permanece auditável.

## Flow tools

### Finalidade

Planejar vários movimentos em uma única proposta ordenada.

### Campos

- **Unidades**: identificadores das unidades selecionadas.
- **Destinos**: slots, tiers, pilhas ou posições que receberão as unidades.
- **Padrão Stack-wise**: completa uma pilha antes de avançar para a seguinte.
- **Padrão Tier-wise**: completa o mesmo tier entre pilhas antes de subir.
- **Sentido crescente ou decrescente**: define a direção da ordenação.
- **Paired 20**: forma pares somente com unidades de 20 pés.
- **Alternar bays**: alterna bays das extremidades para reduzir concentração operacional.

### Estados

- Rascunho;
- proposta;
- encaminhado;
- cancelado;
- bloqueado.

### Bloqueios

- nenhuma unidade selecionada;
- nenhum destino selecionado;
- quantidade insuficiente de destinos;
- destino duplicado;
- unidade incompatível com Paired 20;
- validação definitiva do backend.

### Exemplo

Selecione seis contêineres de 20 pés, oito destinos e o padrão Stack-wise. Ative Paired 20 e alternância de bays. Revise a numeração e confirme o lote. O comando é persistido uma única vez, mesmo que a mesma requisição seja reenviada.

## Perfil do navio e Quay Commander

### Finalidade

Exibir work queues sobre os bays e simular divisão ou transferência entre guindastes.

### Campos

- bay;
- guindaste;
- quantidade planejada;
- quantidade restante;
- estado da fila;
- motivo de bloqueio;
- produtividade;
- término previsto;
- ponto da sequência usado para divisão.

### Fluxo

1. Selecione a fila.
2. Informe o guindaste de destino.
3. Informe a posição após a qual a fila será dividida, ou zero para transferir toda a fila.
4. Simule a alteração.
5. Confira a nova sequência e o término previsto.
6. Confirme a programação.

### Bloqueios

- fila inexistente;
- guindaste não informado;
- divisão sem operações resultantes;
- conflito de janela, bay, paralisação ou recurso detectado pelo backend.

## EC Console e CHE

### Finalidade

Representar POWs, pools, filas, jobs, push rate e equipamentos diretamente no mapa.

### Indicadores

- quantidade de POWs;
- quantidade de pools;
- filas;
- jobs totais;
- jobs concluídos;
- jobs bloqueados;
- CHEs ativos;
- CHEs com telemetria atrasada;
- push rate.

### Elementos do CHE

- posição;
- heading;
- conectividade;
- estado;
- unidade transportada;
- job atual;
- próximos jobs;
- trilha;
- rota;
- alcance operacional.

Telemetria acima do limite de atualização é exibida como stale e não pode ser interpretada como posição atual. A alteração de alcance fica bloqueada enquanto a telemetria estiver stale.

## Filtros e recaps bidirecionais

A busca por texto, estado, domínio ou CHE destaca os elementos correspondentes e mantém os demais acinzentados. Nenhum elemento é removido do canvas, preservando o contexto espacial.

Ao selecionar um elemento, recaps, listas e cartões devem refletir a seleção. Ao selecionar uma métrica ou item de lista, os elementos correspondentes devem ser destacados no desenho.

## Workspaces gráficos

### Escopos

- **Individual**: visível ao proprietário.
- **Equipe**: compartilhado com a equipe operacional.
- **Papel**: visível aos usuários com o papel configurado.
- **Padrão**: configuração administrativa padrão do terminal.

### Conteúdo versionado

- paleta;
- filtros;
- atributos;
- painéis;
- posição e dimensão dos painéis;
- visibilidade;
- nome;
- escopo;
- papel.

Salvar nunca sobrescreve a versão anterior. Importação e exportação usam JSON.

## Editor de geometria e rotas

### Elementos físicos

- blocos;
- linhas e pilhas;
- vias;
- trilhos;
- exchange areas;
- transfer points;
- tomadas reefer;
- limites;
- zonas.

### Validações

- identificador obrigatório e único;
- tipo obrigatório;
- dimensões positivas;
- ligações somente entre elementos existentes;
- versão publicada imutável para operações já encerradas.

### Rotas

A rede considera distância, sentido único, bloqueios e congestionamento. O operador pode simular interdições e comparar a rota resultante antes da publicação. A memória de cálculo contém nós, segmentos, custo e ETA.

### Bloqueios

- origem ou destino inexistente;
- rede desconectada;
- todos os caminhos interditados;
- elemento ou ligação inválida;
- publicação concorrente de versão.

## Rail × Yard × Dispatch

### Finalidade

Planejar unidades do pátio para vagões mantendo visíveis work instruction, CHE, linha, posição e sequência.

### Regras

- capacidade restante do vagão;
- tipo de carga permitido;
- peso máximo;
- linha e posição física;
- equipamento associado;
- sequência da atribuição.

### Bloqueios

- ausência de vagão compatível;
- capacidade esgotada;
- tipo incompatível;
- peso acima do limite;
- conflito entre linha, pátio e equipamento;
- work instruction inválida ou bloqueada.

## Estados operacionais

Os elementos usam os estados gráficos padronizados:

- proposta;
- tentativo;
- definitivo;
- reservado;
- atribuído;
- despachado;
- em execução;
- bloqueado;
- falha;
- concluído;
- publicado.

Símbolos, texto, tooltip e `aria-label` devem transmitir o estado sem depender apenas de cor.

## Motivos gerais de bloqueio

- permissão insuficiente;
- origem ou destino ausente;
- unidade divergente;
- posição indisponível;
- conflito de fila ou recurso;
- telemetria stale;
- CHE fora do alcance;
- rota interrompida;
- geometria inválida;
- vagão incompatível;
- versão concorrente;
- comando ou payload inválido.

## Atalhos

- `Ctrl + Enter` ou `Cmd + Enter`: confirma a proposta disponível na aba atual.
- `Esc`: cancela rascunho ou seleção local.
- `F1` ou `Shift + ?`: abre a ajuda contextual da página.
- Duplo clique: aprofunda a navegação hierárquica.
- `Backspace`: retorna um nível preservando o contexto.
- `+` e `-`: alteram o zoom.
- `0`: restaura o viewport.

## Auditoria e idempotência

Cada comando persiste:

- `commandId`;
- tipo;
- status;
- motivo;
- payload integral;
- usuário;
- instante.

O `commandId` é único. Repetir o mesmo comando retorna o registro já processado e não cria uma segunda execução.

Cada workspace persiste:

- nome;
- escopo;
- papel;
- proprietário;
- versão;
- conteúdo integral;
- instante.

## Endpoints

- `POST /api/operacao-2d/comandos`;
- `GET /api/operacao-2d/comandos/{commandId}`;
- `POST /api/operacao-2d/workspaces`;
- `GET /api/operacao-2d/workspaces`.

## Recuperação de falhas

1. Não reenvie manualmente um comando com payload diferente e o mesmo `commandId`.
2. Consulte o comando persistido pelo identificador.
3. Corrija o rascunho no workspace.
4. Gere um novo `commandId`.
5. Reenvie a proposta.
6. Preserve o motivo e a referência do comando anterior para auditoria operacional.
