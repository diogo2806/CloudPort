# Manual operacional — Stuff, unstuff, staging, pesagem e VGM

## Finalidade da tela

A tela controla o planejamento, a programação física, a execução, os lacres, a pesagem e o encerramento de operações de stuffing e unstuffing vinculadas a um contêiner canônico e a um ou mais cargo lots.

A agenda de docas impede que doca, área de espera, recurso, contêiner ou cargo lot sejam comprometidos simultaneamente em operações com janelas sobrepostas. No stuffing, a pesagem também impede a conclusão sem massa bruta verificada ou acima da capacidade máxima.

## Fluxo operacional

1. Crie a operação e selecione o contêiner elegível.
2. Adicione os cargo lots e informe quantidade, volume e peso planejados.
3. Crie a versão do plano e libere a versão mais recente.
4. Abra **Agenda de docas e staging**.
5. Selecione a operação planejada.
6. Informe doca, área de espera, recurso e início e fim da janela operacional.
7. Reserve a programação. O sistema valida sobreposição de todos os recursos e lotes.
8. Aguarde a abertura da janela.
9. Inicie a operação. A programação muda de **RESERVADA** para **EM_USO**.
10. Registre a execução física dos itens.
11. Registre e confira os lacres aplicáveis.
12. Para stuffing, confirme tara, peso bruto, método, equipamento, responsável, VGM e capacidade máxima.
13. Conclua a operação. A doca, a área de espera, o recurso, o contêiner e os cargo lots são liberados na mesma transação.
14. Quando a operação não puder continuar, cancele-a com motivo; os saldos são compensados e a programação é liberada.

## Explicação dos campos

### Operação e planejamento

- **Operação**: define stuffing ou unstuffing.
- **Contêiner canônico**: unidade reservada no inventário para a operação.
- **Armazém**: instalação relacionada à movimentação da carga geral.
- **Local da operação**: referência física complementar da execução.
- **Equipe ou recurso**: informação operacional geral da ordem.
- **Cargo lot**: lote de carga movimentado e reservado na agenda.
- **Quantidade, volume e peso planejados**: limites da execução e valores reservados para cada lote.
- **Versão do plano**: fotografia imutável dos cargo lots e valores planejados.

### Agenda de docas e staging

- **Operação planejada**: operação cujo plano já pode ser relacionado à programação física.
- **Doca**: posição física exclusiva utilizada durante a janela.
- **Área de espera**: área de staging reservada para preparação da carga ou do contêiner.
- **Recurso operacional**: equipe, empilhadeira, guindaste, reach stacker ou outro recurso exclusivo da programação.
- **Início da janela**: instante a partir do qual a operação pode ser iniciada.
- **Fim da janela**: limite para iniciar a operação; depois desse instante é necessária reprogramação.
- **Observação da reserva**: detalhe operacional da programação.
- **Cargo lots reservados**: lotes do plano liberado protegidos contra outra programação sobreposta.
- **Ocupação**: indica se os recursos ainda estão reservados ou já foram liberados.

### Execução

- **Command ID**: identificador idempotente do apontamento.
- **Quantidade realizada**: parcela física executada.
- **Volume realizado**: volume físico executado em metros cúbicos.
- **Peso realizado**: peso físico executado em quilogramas.
- **Avaria e divergência**: ocorrências encontradas durante a execução.

### Pesagem e VGM

- **Método 1**: o contêiner carregado é pesado diretamente; peso bruto e VGM devem coincidir dentro da tolerância.
- **Método 2**: o VGM corresponde à tara somada ao peso executado da carga.
- **Tara kg**: massa do contêiner vazio.
- **Peso bruto kg**: massa total apurada do contêiner carregado.
- **VGM kg**: massa bruta verificada usada para autorizar a continuidade operacional.
- **Capacidade máxima kg**: limite operacional considerado na confirmação.
- **Equipamento de pesagem**: balança ou equipamento responsável pela medição.
- **Responsável pela pesagem**: pessoa que conferiu a medição.
- **Observação**: informação complementar da pesagem.

### Encerramento

- **Lacre final**: lacre aplicado ao término da operação.
- **Observação de conclusão**: registro complementar do encerramento.
- **Motivo do cancelamento**: justificativa obrigatória para compensar saldos e liberar recursos.

## Permissões necessárias

- **ADMIN_PORTO**: consulta, planejamento, programação, execução, pesagem, overrides autorizados de lacre, conclusão e cancelamento conforme o endpoint.
- **PLANEJADOR**: criação e versionamento do plano, liberação, reserva, reprogramação e cancelamento da programação antes do início, consulta e cancelamento da operação.
- **OPERADOR_GATE**: consulta da agenda, início dentro da janela, execução, registro de lacres sem override administrativo, pesagem e conclusão.

O backend valida a permissão de cada comando mesmo quando a interface estiver visível para o perfil.

## Estados possíveis

### Operação

- **PLANEJADA**: criada e aguardando liberação do plano, programação ou início.
- **EM_EXECUCAO**: iniciada e apta a receber apontamentos.
- **PARCIAL**: possui execução física parcial.
- **CONCLUIDA**: critérios atendidos, inventário e recursos liberados.
- **CANCELADA**: encerrada com compensação e liberação dos recursos.

### Programação de doca

- **RESERVADA**: doca, área de espera, recurso, contêiner e cargo lots estão protegidos durante a janela.
- **EM_USO**: a operação iniciou dentro da janela e os recursos permanecem ocupados.
- **CONCLUIDA**: a operação terminou e os recursos foram liberados.
- **CANCELADA**: a programação foi liberada antes do início ou pelo cancelamento da operação.

### Pesagem VGM

- **PENDENTE**: nenhuma confirmação válida foi registrada.
- **CONFIRMADA**: pesagem consistente e dentro da capacidade máxima.
- **BLOQUEADA_EXCESSO**: o VGM excede a capacidade máxima e impede a conclusão.

## Motivos de bloqueio

### Planejamento e staging

- A versão mais recente do plano não está liberada.
- A operação já iniciou, foi concluída ou foi cancelada.
- Existe programação ativa e o usuário tenta alterar os cargo lots do plano.
- Doca já reservada em uma janela sobreposta.
- Área de espera já reservada em uma janela sobreposta.
- Recurso operacional já reservado em uma janela sobreposta.
- Contêiner já programado em uma janela sobreposta.
- Um ou mais cargo lots já programados em uma janela sobreposta.
- Início ou fim da janela ausente.
- Fim da janela igual ou anterior ao início.
- Janela inteiramente expirada.
- Tentativa de iniciar antes da abertura da janela.
- Tentativa de iniciar depois do término da janela.
- Tentativa de registrar execução sem programação em uso.
- Tentativa de cancelar isoladamente uma programação que já está em uso.

### Lacres, pesagem e encerramento

- Um ou mais itens ainda não atingiram a execução integral.
- Operação de unstuffing enviada à confirmação de VGM.
- Método, valores, equipamento ou responsável da pesagem ausentes.
- Peso bruto menor que a tara.
- Diferença superior a 1 kg entre peso bruto e VGM.
- No método 2, diferença superior a 1 kg entre VGM e tara mais carga executada.
- VGM superior à capacidade máxima.
- Divergência de lacre aberta sem override autorizado.
- Perfil sem permissão para executar a ação.

## Exemplos

### Reserva sem conflito

- Operação: stuffing do contêiner `CONT-001`.
- Doca: `DOCA-01`.
- Área de espera: `AREA-A`.
- Recurso: `EMPILHADEIRA-07`.
- Janela: 20/07/2026, das 08:00 às 10:00.
- Resultado: programação **RESERVADA** e início permitido somente dentro da janela.

### Conflito de recurso

- A `EMPILHADEIRA-07` já está reservada das 08:00 às 10:00.
- Uma segunda operação tenta reservá-la das 09:30 às 11:00.
- Resultado: HTTP 409; nenhuma reserva parcial é persistida.

### Reprogramação

- A operação ainda está planejada e a programação está **RESERVADA**.
- O planejador altera doca ou janela.
- Resultado: o mesmo agregado é atualizado após nova validação integral de conflitos.

### Pesagem liberada pelo método 2

- Tara: 2.200 kg.
- Peso executado total: 22.300 kg.
- VGM e peso bruto: 24.500 kg.
- Capacidade máxima: 30.480 kg.
- Resultado: pesagem **CONFIRMADA**.

### Pesagem bloqueada por excesso

- VGM: 31.000 kg.
- Capacidade máxima: 30.480 kg.
- Resultado: **BLOQUEADA_EXCESSO** e conclusão impedida.

## Atalhos

- **Manual**: abre este documento.
- **Atualizar dados**: recarrega cargo lots e contêineres elegíveis.
- **Planejar e operar**: abre plano, execução e encerramento.
- **Abrir programação**: seleciona a operação na agenda.
- **Reservar recursos**: valida e persiste a programação completa.
- **Reprogramar recursos**: altera uma reserva ainda não iniciada.
- **Cancelar programação**: libera uma reserva antes do início.
- **Abrir pesagem**: carrega a etapa de pesagem e VGM.
- **F1** ou **Shift + ?**: abre a ajuda contextual global quando disponível.

## Processo completo

O fluxo está relacionado aos requisitos BUS1380 e BUS1390 e ao registro canônico de funcionalidades implementadas:

- [Requisitos técnicos pendentes](../requisitos/requisito-tecnico.md)
- [Requisitos implementados](../implementados/requisitos-implementados.md)
