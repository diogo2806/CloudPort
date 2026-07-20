# Fila de pré-gate

## Finalidade da tela

A fila de pré-gate organiza veículos que chegaram antes da entrada física no terminal. Ela mantém posição, prioridade, chamada, aceite, expiração, rechamada, cancelamento e início do atendimento de forma persistida e auditável.

## Fluxo operacional

1. Confirme a chegada antecipada do agendamento.
2. O sistema cria ou reutiliza o GatePass e inclui o veículo uma única vez na fila de entrada.
3. Confira a posição atual, a posição original e a prioridade.
4. Selecione o veículo e informe o gate ou a pista e a validade da chamada.
5. Aguarde o aceite do motorista dentro do tempo restante.
6. Em caso de expiração, efetue a rechamada ou mantenha o veículo aguardando.
7. Após o aceite, inicie o atendimento.
8. A entrada física remove o veículo da fila de entrada e cria a etapa de saída sem duplicar a visita ativa.
9. Finalize ou cancele o chamado conforme o resultado operacional.

## Explicação dos campos

- **Posição atual:** ordem utilizada no atendimento, considerando prioridade e reordenações autorizadas.
- **Posição original:** ordem recebida quando o veículo entrou na fila.
- **GatePass:** identificador da passagem vinculada ao agendamento.
- **Sentido:** entrada ou saída.
- **Prioridade:** normal, alta ou emergencial.
- **Status da fila:** aguardando, chamado ou em atendimento.
- **Gate ou pista:** local para o qual o motorista deve se dirigir.
- **Validade:** período, entre 1 e 60 minutos, disponível para o aceite.
- **Tempo restante:** contagem regressiva até a expiração da chamada.
- **Aceite:** instante em que o motorista confirmou a chamada.
- **Rechamadas:** quantidade de novas tentativas após expiração.
- **Justificativa:** motivo obrigatório para reordenação, prioridade especial ou cancelamento.

## Permissões necessárias

- **ADMIN_PORTO:** consulta, chamada, aceite, expiração, rechamada, cancelamento, reordenação e alteração de prioridade.
- **OPERADOR_GATE:** execução do ciclo operacional da fila e dos chamados.
- **PLANEJADOR:** confirmação da chegada antecipada conforme autorização do endpoint de agendamento.

## Estados possíveis

### Fila

- **AGUARDANDO:** veículo posicionado e ainda não chamado.
- **CHAMADO:** motorista convocado, aguardando aceite.
- **EM_ATENDIMENTO:** aceite concluído e atendimento iniciado.

### Chamada

- **CHAMADO:** chamada ativa dentro da validade.
- **ACEITO:** motorista aceitou e pode iniciar o atendimento.
- **EM_ATENDIMENTO:** atendimento operacional em andamento.
- **FINALIZADO:** atendimento concluído.
- **EXPIRADO:** o prazo terminou sem aceite.
- **CANCELADO:** chamada interrompida com justificativa.

## Motivos de bloqueio

- Agendamento cancelado, concluído ou marcado como não comparecimento.
- GatePass inexistente.
- Veículo fora de uma fila ativa.
- Outra chamada ativa para o mesmo GatePass.
- Tentativa de aceitar uma chamada expirada.
- Transição incompatível com o estado atual.
- Gate ou pista não informado.
- Validade fora do intervalo de 1 a 60 minutos.
- Prioridade especial, reordenação ou cancelamento sem justificativa.
- Perfil sem permissão operacional.

## Exemplos

### Chegada antecipada normal

O agendamento é confirmado antes da janela e recebe a posição 8. O operador chama o veículo para a Pista 2 com validade de 5 minutos. O motorista aceita, o atendimento começa e a entrada física remove o GatePass da fila de entrada.

### Chamada expirada

O veículo está na posição 3 e não aceita dentro de 5 minutos. A chamada passa para **EXPIRADO**, a fila retorna para **AGUARDANDO** e o operador pode rechamar sem criar outra visita ativa.

### Prioridade emergencial

Um veículo recebe prioridade **EMERGENCIAL** com justificativa registrada. A ordenação considera a prioridade antes da posição FIFO, preservando a posição original para auditoria.

## Atalhos

- **F1:** abrir a ajuda da tela.
- **Shift + ?:** abrir a ajuda da tela.
- **Esc:** fechar painéis e diálogos.
- **Atualizar agora:** recarregar filas, chamados e pistas.
- **Pré-gate:** abrir diretamente a visão consolidada da fila.

## Processo completo

Acesse **Gate > Central de ação do Gate** em `/home/gate/dashboard`. O processo relacionado à entrada física continua em **Gate > Operação completa**, em `/home/gate/operacao`.
