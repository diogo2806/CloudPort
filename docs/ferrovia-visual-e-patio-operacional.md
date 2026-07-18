# Ferrovia visual e pátio operacional

## 1. Ferrovia visual

### Finalidade da tela

A tela de visitas ferroviárias passa a usar o workspace visual completo da ferrovia. Ela reúne composição do trem, ocupação das linhas, planejamento de vagões, progresso operacional, cronograma e conflitos.

### Fluxo operacional

1. Selecione a janela de visitas.
2. Escolha o instante real ou simulado.
3. Selecione uma visita de trem.
4. Consulte locomotiva, vagões, contêineres, carga e descarga.
5. Arraste vagões entre linhas para simular o planejamento.
6. Arraste contêineres entre vagões e informe o motivo para persistir o replanejamento.
7. Revise conflitos de linha e recurso no cronograma.

### Campos e elementos

- **Visita de trem**: composição ativa.
- **Linha**: linha planejada para o vagão.
- **Progresso**: percentual de operações concluídas no vagão.
- **Carga/descarga**: quantidade de operações associadas.
- **Bloqueado**: vagão indisponível para movimentação.
- **Incompatível**: vagão que não atende às condições da operação.
- **Cronograma**: recepção, operação e expedição por linha.

### Permissões necessárias

A consulta segue as permissões do módulo ferroviário. Replanejamentos e comandos persistidos exigem perfil operacional autorizado pelo backend.

### Estados possíveis

- previsto;
- recepção;
- operação;
- expedição;
- concluído;
- bloqueado;
- incompatível;
- conflito de linha ou recurso.

### Motivos de bloqueio

- vagão bloqueado na origem;
- operação concluída;
- incompatibilidade do vagão;
- ausência de motivo operacional;
- conflito de versão da composição;
- restrição retornada pelo backend.

### Exemplos

- mover o vagão `VAG-12` da Linha 1 para a Linha 3;
- transferir um contêiner de descarga para outro vagão compatível;
- identificar dois trens planejados na mesma linha no mesmo período.

### Atalhos

- `−6h`, `Agora`, `+6h` e `+24h` alteram o instante simulado;
- clicar em uma visita na tabela abre sua composição;
- arrastar o vagão sobre uma linha altera o plano visual;
- arrastar o contêiner entre vagões abre a confirmação do replanejamento.

### Processo completo

Visita ferroviária → composição → planejamento de linhas e vagões → operação de carga/descarga → acompanhamento do cronograma → resolução de conflitos → partida.

## 2. Pátio operacional

### Finalidade da tela

O mapa do pátio consolida georreferenciamento, vistas operacionais, heatmaps, reefers, CHEs, rotas, allocations, movimentação manual, restrições, workspaces e simulação.

### Fluxo operacional

1. Aplique os filtros do Yard.
2. Selecione uma pilha no Google Maps ou em qualquer vista operacional.
3. Alterne entre bloco, seção lateral, scan e microvisão.
4. Escolha a camada de situação, ocupação, dwell time ou reefer.
5. Consulte CHEs, rotas, áreas bloqueadas e interditadas.
6. Arraste um contêiner para uma posição livre.
7. Revise a simulação, informe o motivo e confirme.
8. Edite notas, bloqueios e interdições da pilha quando autorizado.
9. Salve o conjunto de filtros e vista como workspace.

### Campos e elementos

- **Bloco, linha, coluna e camada**: endereço operacional.
- **Ocupação**: camadas utilizadas e capacidade.
- **Dwell time**: maior permanência da pilha.
- **Reefer**: quantidade e alarmes de temperatura.
- **CHE**: equipamento, status e posição atual.
- **Rota**: origem e destino da work instruction.
- **Allocation**: destino planejado.
- **Nota operacional**: observação editável da pilha.

### Permissões necessárias

- `ADMIN_PORTO`, `PLANEJADOR` e `OPERADOR_PATIO`: movimentação, allocations, notas e restrições;
- demais perfis: consulta das vistas e indicadores.

### Estados possíveis

- disponível;
- ocupação parcial;
- completa;
- reservada;
- bloqueada;
- interditada;
- alerta reefer;
- simulação válida ou inválida.

### Motivos de bloqueio

- posição ocupada;
- pilha ou área bloqueada;
- área interditada;
- allocation incompatível;
- ausência de motivo operacional;
- limite de camada, peso ou tipo de carga;
- conflito de work instruction;
- backend não confirmou a simulação.

### Exemplos

- localizar um RTG no mapa e selecionar sua pilha mais próxima;
- visualizar ocupação acima de 80% no heatmap;
- mover manualmente um contêiner após validar a simulação;
- interditar uma pilha e registrar a justificativa;
- salvar um workspace de reefers críticos de um bloco.

### Atalhos

- clicar no marcador do CHE seleciona a pilha associada;
- clicar no mapa sincroniza a seleção com bloco, seção, scan e microvisão;
- arrastar uma camada ocupada inicia a simulação;
- salvar vista preserva filtros, bloco, linha, vista e overlay no navegador.

### Processo completo

Inventário → seleção da pilha → análise de ocupação/dwell/reefer → planejamento ou allocation → simulação → confirmação motivada → acompanhamento do CHE e da work instruction.
