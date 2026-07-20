# Ferrovia — manobras e inspeções de vagões

## Finalidade

A tela Ferrovia visual permite planejar e executar manobras ferroviárias com reserva concorrente de linha e trecho, além de inspecionar cada vagão antes da carga ou descarga.

O fluxo evita duas condições críticas:

1. duas manobras ativas utilizando simultaneamente o mesmo trecho da mesma linha;
2. carga ou descarga em vagão sem inspeção válida, reprovado ou sem override autorizado.

## Fluxo operacional

### 1. Selecionar a visita

Selecione uma visita de trem na seção Composição gráfica e planejamento. A tela carrega a locomotiva, os vagões, os contêineres, o plano de manobras e o histórico de inspeções da visita.

### 2. Inspecionar os vagões

Para cada vagão:

1. selecione o identificador do vagão;
2. informe o responsável pela inspeção;
3. confirme rodas, freios, engates, estrutura e lacres;
4. registre observações;
5. quando houver não conformidade, informe código, descrição, severidade e evidência;
6. confirme a inspeção.

Uma inspeção é aprovada somente quando os cinco itens estão conformes e não existem defeitos registrados.

### 3. Tratar reprovação

O vagão reprovado não é elegível para carga ou descarga e suas ordens deixam de aparecer na lista operacional.

Há duas formas de liberação:

1. realizar nova inspeção após a correção do defeito;
2. aplicar override autorizado, informando responsável e motivo operacional.

O override não apaga a reprovação. A inspeção passa ao estado `LIBERADA_OVERRIDE`, preservando responsável, motivo e data da liberação.

### 4. Planejar a manobra

Informe:

- sequência;
- origem;
- destino;
- composição movimentada;
- linha;
- trecho;
- início previsto;
- fim previsto.

Ao confirmar, o backend verifica todas as manobras ativas da mesma linha e do mesmo trecho.

Quando não existe sobreposição, a manobra fica `PLANEJADA` e a reserva é protegida também por restrição transacional no banco.

Quando existe sobreposição, a manobra fica `BLOQUEADA_CONFLITO` e apresenta a manobra conflitante e sua janela.

### 5. Executar a manobra

A sequência permitida é:

1. `PLANEJADA` ou `BLOQUEADA_CONFLITO` para `AUTORIZADA`;
2. `AUTORIZADA` para `EM_EXECUCAO`;
3. `EM_EXECUCAO` para `CONCLUIDA`.

A autorização revalida a ocupação. Uma manobra anteriormente bloqueada somente é autorizada quando o trecho estiver livre.

Manobras planejadas, bloqueadas ou autorizadas podem ser canceladas com motivo obrigatório.

## Explicação dos campos

### Plano de manobra

| Campo | Explicação |
|---|---|
| Sequência | Ordem operacional da etapa dentro da visita. Não pode se repetir na mesma visita. |
| Origem | Posição inicial da composição. |
| Destino | Posição final da composição. |
| Composição | Locomotiva, vagões ou agrupamento que será movimentado. |
| Linha | Linha ferroviária utilizada. |
| Trecho | Segmento físico reservado dentro da linha. |
| Início previsto | Início da ocupação exclusiva do trecho. |
| Fim previsto | Fim da ocupação exclusiva do trecho. Deve ser posterior ao início. |
| Conflito | Descrição da reserva ativa que impede a autorização. |
| Autorizado por | Usuário autenticado que autorizou a etapa. |
| Iniciado em | Instante real de início da execução. |
| Concluído em | Instante real de conclusão. |

### Inspeção de vagão

| Campo | Explicação |
|---|---|
| Vagão | Identificador pertencente à composição selecionada. |
| Rodas | Condição de rodas, eixos e elementos visíveis associados. |
| Freios | Condição operacional do sistema de frenagem. |
| Engates | Condição de engates e conexões entre veículos. |
| Estrutura | Condição estrutural do vagão. |
| Lacres | Conformidade dos lacres aplicáveis. |
| Responsável | Inspetor que realizou a verificação física. |
| Observação | Informação complementar da inspeção. |
| Código do defeito | Código operacional da não conformidade. |
| Descrição | Detalhamento do defeito encontrado. |
| Severidade | Baixa, média, alta ou crítica. |
| Evidência | URL, arquivo ou referência que comprova o defeito. |

## Permissões necessárias

A consulta exige usuário autenticado no módulo ferroviário.

A criação de inspeções e manobras exige permissão operacional de escrita no runtime.

A autorização de manobra e o override de inspeção devem ser realizados somente por responsável operacional habilitado conforme a política do terminal. O backend registra o usuário autenticado na autorização e os dados informados na liberação excepcional.

## Estados possíveis

### Manobra

- `PLANEJADA`: trecho reservado, aguardando autorização;
- `BLOQUEADA_CONFLITO`: sobreposição detectada, sem autorização possível;
- `AUTORIZADA`: ocupação revalidada e liberada;
- `EM_EXECUCAO`: movimentação iniciada;
- `CONCLUIDA`: movimentação finalizada;
- `CANCELADA`: etapa encerrada antes da execução, com motivo.

### Inspeção

- `APROVADA`: checklist conforme e sem defeitos;
- `REPROVADA`: item não conforme ou defeito registrado;
- `LIBERADA_OVERRIDE`: reprovação mantida no histórico, mas operação excepcionalmente autorizada.

## Motivos de bloqueio

- linha e trecho reservados por outra manobra ativa no mesmo período;
- sequência duplicada na mesma visita;
- fim previsto igual ou anterior ao início;
- transição de estado fora da sequência operacional;
- cancelamento sem motivo;
- vagão não pertencente à composição;
- vagão sem inspeção registrada;
- última inspeção reprovada;
- ordem sem vagão associado ao manifesto;
- alteração concorrente da reserva entre validação e persistência.

## Exemplos

### Reserva sem conflito

A manobra 1 utiliza a Linha 1, trecho Recepção–Pátio A, das 13h às 14h. A manobra 2 pode usar o mesmo trecho a partir das 14h, pois as janelas não se sobrepõem.

### Reserva conflitante

A manobra 1 utiliza a Linha 1, trecho Pátio A–Moega, das 14h às 15h. Uma nova manobra no mesmo trecho das 14h30 às 15h30 é registrada como bloqueada por conflito.

### Vagão reprovado

O vagão VAG-001 apresenta defeito crítico no freio. A inspeção fica reprovada e suas ordens deixam de ser elegíveis. Após manutenção, uma nova inspeção aprovada substitui a reprovação como inspeção mais recente.

### Override

Quando a política operacional permitir uma liberação excepcional, o responsável registra seu nome e o motivo. O estado passa a `LIBERADA_OVERRIDE`, sem remover o defeito nem a inspeção reprovada do histórico.

## Atalhos

- F1 ou Shift + ?: abrir a ajuda contextual da aplicação;
- Atualizar ferrovia: recarregar o line-up;
- Atualizar operação: recarregar manobras e inspeções da visita;
- Enter em um formulário: confirmar o registro quando os campos obrigatórios estiverem preenchidos;
- clicar em Autorizar, Iniciar ou Concluir: executar a próxima transição válida da manobra.

## APIs

- `GET /rail/ferrovia/lista-trabalho/visitas/{idVisita}/manobras`
- `POST /rail/ferrovia/lista-trabalho/visitas/{idVisita}/manobras`
- `PATCH /rail/ferrovia/lista-trabalho/visitas/{idVisita}/manobras/{idManobra}/status`
- `GET /rail/ferrovia/lista-trabalho/visitas/{idVisita}/inspecoes-vagoes`
- `POST /rail/ferrovia/lista-trabalho/visitas/{idVisita}/inspecoes-vagoes`
- `PATCH /rail/ferrovia/lista-trabalho/visitas/{idVisita}/inspecoes-vagoes/{idInspecao}/override`
- `GET /rail/ferrovia/lista-trabalho/visitas/{idVisita}/ordens`
- `PATCH /rail/ferrovia/lista-trabalho/visitas/{idVisita}/ordens/{idOrdem}/status`
