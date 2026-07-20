const MANUAL_URL = 'https://github.com/diogo2806/CloudPort/blob/main/docs/manuais/patio-planejamento-preditivo-yard-impact.md';
const DISPATCH_MANUAL_URL = 'https://github.com/diogo2806/CloudPort/blob/main/docs/manuais/patio-dispatch-equipamentos.md';

const HELP_BY_PATH = {
  '/home/patio/planejamento-recebimento': {
    title: 'Recebimento integrado Gate × Yard',
    purpose: 'Selecionar uma truck visit, validar suas referências comerciais e simular o agrupamento das unidades elegíveis antes de reservar posição, exchange area e work instruction.',
    flow: [
      'Selecione a instalação e uma truck visit ativa carregada pelo Gate.',
      'Revise as transações, unidades, bookings, Bills of Lading, ordens e pré-avisos vinculados.',
      'Corrija no Gate transações sem unidade, com trouble, em estado final ou sem autorização comercial compatível.',
      'Simule o planejamento somente com as transações elegíveis.',
      'Revise grupos, segregações, alertas e propostas de posição.',
      'Confirme reservas e work instructions somente pelo fluxo transacional Gate × Yard; a simulação não conclui essas etapas.'
    ],
    fields: [
      'Truck visit: visita operacional que fornece veículo, transportadora, pista, estágio e transações.',
      'Referência comercial: booking, Bill of Lading, ordem ou pré-aviso usado para justificar a movimentação.',
      'Elegibilidade: resultado da validação anterior ao planejamento.',
      'Grupo sugerido: conjunto compatível por fluxo, armador, viagem, equipamento, peso e segregação.',
      'Posição: proposta persistida como TENTATIVO, DEFINITIVO ou IMINENTE no painel preditivo.',
      'Exchange area e work instruction: etapas ainda pendentes enquanto não houver confirmação do orquestrador.'
    ],
    permissions: [
      'ADMIN_PORTO: consulta completa, simulação e operações autorizadas nos módulos Gate e Yard.',
      'PLANEJADOR: consulta das visitas, simulação, revisão e conversão dos planos de posição.',
      'OPERADOR_PATIO: consulta e execução após a confirmação operacional.',
      'OPERADOR_GATE: manutenção da visita, transações e referências conforme autorização do Gate.'
    ],
    states: [
      'ELEGÍVEL: transação apta a participar da simulação.',
      'BLOQUEADA: transação excluída por inconsistência ou ausência de autorização.',
      'NÃO SIMULADA: nenhuma proposta foi calculada para a visita selecionada.',
      'SIMULADA: agrupamento calculado, sem reserva operacional confirmada.',
      'TENTATIVO, DEFINITIVO, IMINENTE, EXPIRADO ou CANCELADO: estados persistidos do plano de posição.'
    ],
    blockers: [
      'Unidade não identificada na transação, ordem ou pré-aviso.',
      'Trouble ativo.',
      'Transação cancelada, concluída ou finalizada.',
      'Importação sem Bill of Lading ou ordem.',
      'Exportação ou vazio sem booking, ordem ou pré-aviso.',
      'ISO type ou peso ausente exige validação antes da posição definitiva.',
      'Perfil sem permissão ou versão do plano alterada concorrentemente.'
    ],
    example: 'Uma truck visit de exportação possui duas transações. A primeira tem unidade, booking e pré-aviso e entra na simulação. A segunda está com trouble e aparece bloqueada com o motivo, sem ser enviada ao agrupador.',
    shortcuts: ['F1 ou Shift + ?: abrir esta ajuda.', 'Esc: fechar a ajuda.', 'Atualizar: recarregar visitas e referências.', 'Simular planejamento: calcular apenas as transações elegíveis.', 'Clique no grupo: abrir o inspector da decisão.'],
    processPath: '/home/patio/yard-impact',
    processLabel: 'Abrir Yard Impact',
    documentationUrl: MANUAL_URL
  },
  '/home/patio/yard-impact': {
    title: 'Yard Impact',
    purpose: 'Projetar por bloco e POW o impacto operacional das próximas seis a vinte e quatro horas.',
    flow: [
      'Escolha o horizonte, respeitando o mínimo de seis horas.',
      'Revise entradas, saídas, rehandles, reservas e work instructions.',
      'Identifique blocos saturados no mapa de calor.',
      'Confira demanda, cobertura e déficit de CHE por POW.',
      'Selecione bloco ou POW e desça até as unidades que geram o impacto.'
    ],
    fields: [
      'Horizonte em horas: período da projeção.',
      'Ocupação projetada: contêineres atuais, reservas, planos, entradas e saídas.',
      'Saturação: ocupação projetada igual ou superior a 85%.',
      'Demanda de CHE: quantidade estimada para as work instructions.',
      'Déficit: diferença entre demanda e equipamentos operacionais associados.',
      'Drill-down: unidade, posição, estado, equipamento, validade, origem e motivo.'
    ],
    permissions: [
      'ADMIN_PORTO: consulta completa.',
      'PLANEJADOR: consulta e uso no planejamento.',
      'OPERADOR_PATIO: consulta para preparação e execução.'
    ],
    states: ['CONTROLADO: bloco abaixo do limite de saturação.', 'SATURADO: bloco com ocupação projetada a partir de 85%.', 'COBERTO: POW com recursos suficientes.', 'BLOQUEADO: POW com ausência de cobertura ou déficit de CHE.'],
    blockers: ['Bloco sem capacidade cadastrada.', 'POW ou pool operacional ausente.', 'CHE real não associado.', 'Equipamento associado indisponível.', 'Déficit de CHE no horizonte.', 'Plano vencido ou sem posição válida.'],
    example: 'Ao projetar seis horas, um bloco com 88% aparece SATURADO. Selecione-o para listar as unidades, reservas e posições responsáveis pela pressão operacional.',
    shortcuts: ['F1 ou Shift + ?: abrir esta ajuda.', 'Esc: fechar a ajuda.', 'Atualizar projeção: recalcular com dados persistidos.', 'Clique no bloco ou POW: filtrar o drill-down.'],
    processPath: '/home/patio/planejamento-recebimento',
    processLabel: 'Abrir planejamento preditivo',
    documentationUrl: MANUAL_URL
  },
  '/home/patio/dispatch-equipamentos': {
    title: 'Dispatch e equipamentos',
    purpose: 'Ordenar e atribuir work instructions conforme família de CHE, prioridade, atraso, rota, telemetria, capacidade e restrições operacionais.',
    flow: [
      'Selecione uma work queue ativa com POW, pool e CHE real associado.',
      'Calcule o ranking e revise score, ETA, rota, telemetria e motivos de bloqueio.',
      'Selecione uma work instruction elegível e confirme o autodespacho.',
      'Acompanhe as seis etapas persistidas até a confirmação física.',
      'No modo automático, permita que a conclusão ou falha VMT acione a próxima seleção.',
      'Administre versões de configuração, rotas, interdições, congestionamento e rollback.'
    ],
    fields: [
      'Work queue: fila operacional que fornece POW, pool, visita, zona e CHE.',
      'Score: soma auditável de prioridade, atraso e ajuste da família, descontando distância e congestionamento.',
      'ETA: deslocamento previsto acrescido dos tempos médios de coleta e entrega.',
      'Telemetria: posição, heading e data de recebimento usadas para validar atualização.',
      'Configuração: escopo, versão, modo, pesos, capacidade, tolerância e seleção de auxiliar.',
      'Rota: origem, destino, distância, sentido, congestionamento, interdição e limite regional.',
      'Auxiliar: chassis, bomb cart, cassette ou acessório reservado para a instrução.'
    ],
    permissions: [
      'ADMIN_PORTO: consulta, autodespacho, configuração, ativação, rollback e rotas.',
      'PLANEJADOR: consulta, autodespacho e manutenção parametrizada.',
      'OPERADOR_PATIO: consulta, ranking, autodespacho e avanço de etapas.',
      'SERVICE_NAVIO: consulta operacional para integração.'
    ],
    states: [
      'RECOMENDADA: decisão calculada antes da atribuição.',
      'ATRIBUÍDA: work instruction despachada para o CHE.',
      'CONCLUÍDA ou FALHA: estado final sincronizado com o VMT.',
      'PENDENTE, EM_EXECUÇÃO, CONCLUÍDA, FALHA ou IGNORADA: estados de cada etapa.',
      'RASCUNHO, ATIVA ou INATIVA: estados de configuração.',
      'RESERVADO, ASSOCIADO, DEVOLVIDO ou CANCELADO: estados do auxiliar.'
    ],
    blockers: [
      'Fila inativa, sem POW, pool, visita, plano ou recurso de cais.',
      'CHE inexistente, divergente, indisponível ou acima da capacidade simultânea.',
      'Visita fora de fase operacional.',
      'Unidade com hold ou instrução concorrente ativa.',
      'Telemetria ausente ou atrasada no modo automático.',
      'Rota interditada, congestionada acima do limite ou região saturada de CHE.',
      'Configuração ativa inexistente ou validade encerrada.',
      'Equipamento auxiliar obrigatório indisponível.'
    ],
    example: 'Uma work queue de trator portuário em modo automático classifica as unidades. A primeira elegível recebe chassis livre do mesmo armador, cria seis etapas e, após a conclusão VMT, libera o chassis e seleciona a próxima instrução.',
    shortcuts: ['F1 ou Shift + ?: abrir esta ajuda.', 'Esc: fechar a ajuda.', 'Calcular ranking: atualizar a ordem recomendada.', 'Clique na linha: selecionar a work instruction.', 'Atualizar: recarregar decisões, configurações e rotas.'],
    processPath: '/home/patio/lista-trabalho',
    processLabel: 'Abrir lista de trabalho',
    documentationUrl: DISPATCH_MANUAL_URL
  }
};

export function applyPredictiveContextHelp(path, baseHelp) {
  const override = HELP_BY_PATH[path];
  if (!override) return baseHelp;
  return { ...baseHelp, ...override, path: baseHelp.path, currentRoles: baseHelp.currentRoles };
}
