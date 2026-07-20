# Manual operacional — Stuff, unstuff, pesagem e VGM

## Finalidade da tela

A tela controla o planejamento, a execução física, os lacres, a pesagem e o encerramento de operações de stuffing e unstuffing vinculadas a um contêiner canônico e a um ou mais cargo lots.

No stuffing, a finalidade adicional da etapa de pesagem é impedir que o contêiner seja concluído e liberado para embarque sem massa bruta verificada, sem responsável identificado ou acima da capacidade máxima informada.

## Fluxo operacional

1. Crie a operação e selecione o contêiner elegível.
2. Adicione os cargo lots e as quantidades, volumes e pesos planejados.
3. Crie e libere a versão válida do plano.
4. Inicie a operação.
5. Registre a execução física de todos os itens.
6. Registre e confira os lacres aplicáveis.
7. Para stuffing, abra a seção **Pesagem e VGM do stuffing**.
8. Informe método, tara, peso bruto, VGM, capacidade máxima, equipamento e responsável.
9. Confirme a pesagem.
10. Corrija os dados quando houver divergência ou bloqueio por excesso.
11. Conclua a operação somente quando a pesagem estiver confirmada e liberada.

## Explicação dos campos

### Operação e planejamento

- **Operação**: define stuffing ou unstuffing.
- **Contêiner canônico**: unidade reservada no inventário para a operação.
- **Armazém**: instalação relacionada à movimentação da carga geral.
- **Local da operação**: posição física onde o stuffing ou unstuffing será executado.
- **Equipe ou recurso**: equipe, máquina ou recurso responsável pela execução.
- **Cargo lot**: lote de carga movimentado.
- **Quantidade, volume e peso planejados**: limites da execução para cada item.

### Execução

- **Command ID**: identificador idempotente do apontamento.
- **Quantidade realizada**: parcela física executada.
- **Volume realizado**: volume físico executado em metros cúbicos.
- **Peso realizado**: peso físico da carga executada em quilogramas.
- **Avaria e divergência**: ocorrências encontradas durante a execução.

### Pesagem e VGM

- **Método 1**: o contêiner já carregado é pesado diretamente. O peso bruto e o VGM devem coincidir dentro da tolerância operacional.
- **Método 2**: o VGM é calculado pela soma da tara com o peso executado de todos os itens da carga.
- **Tara kg**: massa do contêiner vazio.
- **Peso bruto kg**: massa total apurada do contêiner carregado.
- **VGM kg**: massa bruta verificada usada para autorizar a continuidade operacional.
- **Capacidade máxima kg**: limite operacional do contêiner considerado na confirmação.
- **Equipamento de pesagem**: balança, ponte de pesagem ou equipamento responsável pela medição.
- **Responsável pela pesagem**: pessoa que conferiu e assumiu a confirmação operacional.
- **Observação**: informação complementar sobre a medição ou a conferência.

### Encerramento

- **Lacre final**: lacre aplicado ao término da operação.
- **Observação de conclusão**: registro complementar do encerramento.
- **Motivo do cancelamento**: justificativa obrigatória para cancelamento e compensação dos saldos executados.

## Permissões necessárias

- **ADMIN_PORTO**: consulta, planejamento, execução, registro de pesagem, overrides autorizados de lacre, conclusão e cancelamento conforme o endpoint.
- **PLANEJADOR**: criação e versionamento do plano, liberação do plano, consulta da pesagem e cancelamento conforme autorização.
- **OPERADOR_GATE**: início, execução, registro de lacres sem override administrativo, confirmação da pesagem e conclusão.

A interface pode ser exibida para um perfil, mas o backend sempre valida a permissão específica de cada comando.

## Estados possíveis

### Operação

- **PLANEJADA**: criada e aguardando liberação ou início.
- **EM_EXECUCAO**: iniciada e apta a receber apontamentos.
- **PARCIAL**: possui execução física parcial.
- **CONCLUIDA**: todos os critérios foram atendidos e o contêiner foi liberado.
- **CANCELADA**: a operação foi encerrada com compensação dos saldos aplicáveis.

### Pesagem VGM

- **PENDENTE**: nenhuma confirmação válida foi registrada.
- **CONFIRMADA**: pesagem consistente e dentro da capacidade máxima; o stuffing está liberado por peso.
- **BLOQUEADA_EXCESSO**: a pesagem foi registrada, mas o VGM excede a capacidade máxima e impede a conclusão.

## Motivos de bloqueio

- Operação não é do tipo stuffing.
- Operação já está concluída ou cancelada.
- Um ou mais itens ainda não atingiram a execução integral.
- Método de pesagem não informado.
- Tara, peso bruto, VGM ou capacidade máxima ausentes ou menores ou iguais a zero.
- Equipamento de pesagem não informado.
- Responsável pela pesagem não informado.
- Peso bruto menor que a tara.
- Diferença superior a 1 kg entre peso bruto e VGM.
- No método 2, diferença superior a 1 kg entre o VGM e a soma da tara com o peso executado da carga.
- VGM superior à capacidade máxima informada.
- Divergência de lacre aberta sem override autorizado.
- Perfil sem permissão para executar a ação.

## Exemplos

### Exemplo liberado pelo método 1

- Tara: 2.200 kg.
- Peso bruto: 24.500 kg.
- VGM: 24.500 kg.
- Capacidade máxima: 30.480 kg.
- Resultado: pesagem confirmada e stuffing liberado por peso.

### Exemplo liberado pelo método 2

- Tara: 2.200 kg.
- Peso executado total da carga: 22.300 kg.
- VGM informado: 24.500 kg.
- Peso bruto informado: 24.500 kg.
- Capacidade máxima: 30.480 kg.
- Resultado: pesagem confirmada.

### Exemplo bloqueado por excesso

- VGM: 31.000 kg.
- Capacidade máxima: 30.480 kg.
- Resultado: estado **BLOQUEADA_EXCESSO**; a conclusão e a liberação do contêiner permanecem impedidas até nova confirmação válida.

## Atalhos

- **Manual**: abre este documento em uma nova aba.
- **Atualizar dados**: recarrega cargo lots e contêineres elegíveis.
- **Planejar e operar**: abre a operação selecionada para planejamento, execução e conclusão.
- **Abrir pesagem**: carrega a operação na etapa de pesagem e VGM.
- **Confirmar pesagem e VGM**: persiste a medição e calcula o estado de liberação.
- **F1** ou **Shift + ?**: abre a ajuda contextual global quando disponível no portal.

## Processo completo

O processo técnico completo está descrito no requisito BUS1380 e no registro de requisitos implementados do CloudPort:

- [Requisitos técnicos pendentes](../requisitos/requisito-tecnico.md)
- [Requisitos implementados](../implementados/requisitos-implementados.md)
