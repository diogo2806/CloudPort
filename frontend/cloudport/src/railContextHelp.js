const DOCUMENTATION_URL = 'https://github.com/diogo2806/CloudPort/blob/main/docs/manuais/ferrovia-operacao-completa.md';
const MANEUVERS_DOCUMENTATION_URL = 'https://github.com/diogo2806/CloudPort/blob/main/docs/manuais/ferrovia-manobras-inspecoes.md';

export const RAIL_PERMISSIONS = [
  'ADMIN_PORTO: administra cadastros, acompanha a operação e executa ações ferroviárias autorizadas.',
  'PLANEJADOR: cadastra visitas, importa manifestos, planeja composição, linhas e ordens.',
  'OPERADOR_PATIO: consulta a composição e executa movimentações, manobras e inspeções conforme autorização.',
  'O backend valida todas as permissões; a disponibilidade visual de um botão não substitui a autorização.'
];

export const RAIL_VISIT_STATES = [
  'PLANEJADO: visita cadastrada e aguardando chegada.',
  'CHEGOU: trem recebido no terminal.',
  'PROCESSANDO: carga, descarga ou manobra em execução.',
  'CONCLUIDO: todas as operações foram concluídas e a partida pode ser registrada.',
  'PARTIU: visita encerrada após a saída do trem.'
];

export const RAIL_ORDER_STATES = [
  'PENDENTE: ordem criada e aguardando execução.',
  'EM_EXECUCAO: movimentação iniciada.',
  'CONCLUIDA: carga ou descarga confirmada.'
];

export const RAIL_MANEUVER_STATES = [
  'PLANEJADA: manobra registrada e aguardando autorização.',
  'BLOQUEADA_CONFLITO: linha, trecho ou janela em conflito.',
  'AUTORIZADA: reserva revalidada e liberada.',
  'EM_EXECUCAO: manobra iniciada.',
  'CONCLUIDA: manobra finalizada.',
  'CANCELADA: manobra encerrada antes da execução, com motivo.'
];

export const RAIL_INSPECTION_STATES = [
  'APROVADA: checklist conforme e sem defeitos ativos.',
  'REPROVADA: não conformidade ou defeito registrado.',
  'LIBERADA_OVERRIDE: reprovação preservada, com liberação excepcional auditada.'
];

export const RAIL_COMMON_BLOCKERS = [
  'Perfil sem uma das permissões exigidas para a ação.',
  'Visita, trem, vagão ou contêiner não localizado.',
  'Dados obrigatórios ausentes ou formato inválido.',
  'Estado atual incompatível com a transição solicitada.',
  'Alteração concorrente da visita, composição, ordem ou reserva.'
];

const COMMON_SHORTCUTS = [
  'F1: abrir a ajuda da tela ativa quando o foco não estiver em um campo.',
  'Shift + ?: abrir a ajuda da tela ativa quando o foco não estiver em um campo.',
  'Esc: fechar a ajuda e devolver o foco ao botão que a abriu.',
  'Tab e Shift + Tab: percorrer campos e ações.',
  'Enter: confirmar o formulário ativo quando os dados forem válidos.'
];

const RAIL_HELP = {
  '/home/ferrovia/visitas': {
    title: 'Trens e composições',
    purpose: 'Cadastrar e manter visitas ferroviárias, horários previstos e a ordem física dos vagões da composição.',
    flow: [
      'Informe trem, operadora, chegada, partida e estado inicial.',
      'Adicione cada vagão e seu tipo.',
      'Ordene os vagões conforme a posição física no trem.',
      'Salve o cadastro e selecione uma visita existente para revisar ou editar.',
      'Use a importação quando a composição vier de um manifesto eletrônico.'
    ],
    fields: [
      'Identificador do trem: código único da composição ferroviária.',
      'Operadora ferroviária: empresa responsável pelo trem.',
      'Chegada prevista: data e hora planejadas para recepção.',
      'Partida prevista: data e hora planejadas para saída; deve ser posterior à chegada.',
      'Status: fase persistida da visita.',
      'Identificador e tipo do vagão: dados do veículo incluído na composição.',
      'Setas de posição: alteram a ordem física persistida dos vagões.',
      'Remover: retira o vagão do rascunho da composição antes de salvar.',
      'Atualizar: recarrega os registros persistidos.'
    ],
    permissions: RAIL_PERMISSIONS,
    states: RAIL_VISIT_STATES,
    blockers: [
      ...RAIL_COMMON_BLOCKERS,
      'Partida prevista igual ou anterior à chegada prevista.',
      'Identificador de vagão vazio ou repetido na mesma composição.',
      'Tentativa de alteração sem perfil ADMIN_PORTO ou PLANEJADOR.'
    ],
    example: 'Cadastre o trem MRS-2048 com chegada às 08:00, partida às 18:00 e três vagões na ordem física VAG-01, VAG-02 e VAG-03.',
    shortcuts: COMMON_SHORTCUTS,
    processPath: '/home/ferrovia/visitas/importar',
    processLabel: 'Abrir importação de manifesto',
    documentationUrl: `${DOCUMENTATION_URL}#1-cadastro-de-visita-e-composição`
  },
  '/home/ferrovia/visitas/importar': {
    title: 'Importar manifesto ferroviário',
    purpose: 'Enviar um manifesto operacional para criar ou atualizar, de forma validada, a visita, a composição, os vagões e as operações de carga e descarga.',
    flow: [
      'Selecione o arquivo de manifesto aceito pela integração.',
      'Confirme o envio e aguarde a validação do backend.',
      'Revise o resultado retornado, incluindo visita, vagões, cargas e inconsistências.',
      'Corrija o arquivo de origem quando houver erro e envie uma nova versão.',
      'Abra a visita ou a lista de trabalho após uma importação válida.'
    ],
    fields: [
      'Arquivo: manifesto selecionado no dispositivo do usuário.',
      'Importar: envia o arquivo ao backend; fica desabilitado sem arquivo ou durante o processamento.',
      'Resultado da operação: resposta persistida, avisos e identificadores criados ou atualizados.',
      'Mensagens: informam sucesso, formato inválido, inconsistência ou falha de integração.'
    ],
    permissions: RAIL_PERMISSIONS,
    states: [
      'Sem arquivo: nenhuma importação pronta para envio.',
      'Pronto para importar: arquivo selecionado.',
      'Enviando: upload e validação em andamento.',
      'Importado: visita e manifesto persistidos.',
      'Rejeitado: arquivo inválido ou dados inconsistentes.'
    ],
    blockers: [
      ...RAIL_COMMON_BLOCKERS,
      'Arquivo não selecionado, vazio, ilegível ou em formato não aceito.',
      'Manifesto sem trem, operadora, horários, vagões ou operações obrigatórias.',
      'Referências duplicadas ou incompatíveis com a visita existente.',
      'Nova tentativa enquanto outra importação ainda está em processamento.'
    ],
    example: 'Selecione o manifesto da visita MRS-2048. Após a validação, confirme no resultado os três vagões e as ordens de carga e descarga criadas.',
    shortcuts: COMMON_SHORTCUTS,
    processPath: '/home/ferrovia/visitas',
    processLabel: 'Abrir trens e composições',
    documentationUrl: `${DOCUMENTATION_URL}#2-importação-do-manifesto`
  },
  '/home/ferrovia/line-up': {
    title: 'Ferrovia visual e line-up',
    purpose: 'Visualizar a programação ferroviária, simular a ocupação das linhas e planejar a composição, os vagões e os contêineres da visita selecionada.',
    flow: [
      'Defina a janela e a data simulada.',
      'Selecione a visita e confira locomotiva, vagões, contêineres e incompatibilidades.',
      'Distribua vagões nas linhas e bloqueie itens que não podem ser replanejados.',
      'Arraste um contêiner entre vagões e informe o motivo para persistir o replanejamento.',
      'Revise conflitos de linha e execute inspeções e manobras antes da operação.'
    ],
    fields: [
      'Janela: quantidade de dias carregada na simulação.',
      'Data e hora simulada: instante usado para calcular fase e ocupação.',
      'Visita de trem: composição ativa no painel.',
      'Linha do vagão: posição planejada no pátio ferroviário.',
      'Bloquear ou desbloquear: impede ou permite replanejamento local do vagão.',
      'Motivo do replanejamento: justificativa obrigatória para mover um contêiner entre vagões.',
      'Cronograma: segmentos de recepção, operação e expedição por linha.',
      'Conflitos: sobreposições de recurso que exigem replanejamento.',
      'Atualizar ferrovia: recarrega visitas, composição e ocupações.'
    ],
    permissions: RAIL_PERMISSIONS,
    states: [...RAIL_VISIT_STATES, ...RAIL_MANEUVER_STATES],
    blockers: [
      ...RAIL_COMMON_BLOCKERS,
      'Linha ocupada por outra visita no mesmo intervalo.',
      'Vagão bloqueado na origem ou marcado manualmente.',
      'Contêiner concluído, sem vagão compatível ou ausente da composição.',
      'Motivo não informado para confirmar o replanejamento.',
      'Versão da composição alterada por outro usuário.'
    ],
    example: 'Selecione a visita MRS-2048, mova o VAG-02 para a Linha 2 e replaneje o contêiner ABCD1234567 para o VAG-03 com uma justificativa operacional.',
    shortcuts: [
      ...COMMON_SHORTCUTS,
      'Arrastar vagão: alterar a linha no planejamento visual.',
      'Arrastar contêiner: iniciar replanejamento entre vagões.',
      '−6h, Agora, +6h e +24h: navegar no instante simulado.'
    ],
    processPath: '/home/ferrovia/lista-trabalho',
    processLabel: 'Abrir lista de trabalho',
    documentationUrl: `${DOCUMENTATION_URL}#3-line-up-e-planejamento-visual`
  },
  '/home/ferrovia/lista-trabalho': {
    title: 'Lista de trabalho ferroviária',
    purpose: 'Consultar e executar as ordens persistidas de carga e descarga de uma visita ferroviária até a conclusão e a partida do trem.',
    flow: [
      'Escolha a janela, a visita e o filtro de estado.',
      'Selecione uma ordem e confirme contêiner, operação, vagão e visita.',
      'Inicie a ordem pendente.',
      'Conclua a ordem em execução após a confirmação física.',
      'Registre a partida somente quando a visita estiver concluída.'
    ],
    fields: [
      'Janela de visitas: período usado para carregar visitas ferroviárias.',
      'Visita de trem: manifesto e conjunto de ordens em contexto.',
      'Status das ordens: filtra pendentes, em execução ou concluídas.',
      'Busca: localiza por contêiner, operação ou estado.',
      'Ordem: contêiner, tipo de movimentação, estado e datas.',
      'Iniciar movimentação: transiciona PENDENTE para EM_EXECUCAO.',
      'Concluir movimentação: transiciona EM_EXECUCAO para CONCLUIDA.',
      'Registrar partida: encerra uma visita no estado CONCLUIDO.'
    ],
    permissions: RAIL_PERMISSIONS,
    states: [...RAIL_ORDER_STATES, ...RAIL_VISIT_STATES],
    blockers: [
      ...RAIL_COMMON_BLOCKERS,
      'Vagão sem inspeção aprovada ou liberada por override.',
      'Ordem sem associação válida no manifesto.',
      'Tentativa de concluir uma ordem ainda pendente.',
      'Ordens não concluídas impedindo o encerramento da visita.',
      'Visita diferente de CONCLUIDO ao registrar a partida.'
    ],
    example: 'Na visita MRS-2048, selecione a descarga do contêiner ABCD1234567, inicie a movimentação e conclua após a retirada física do vagão.',
    shortcuts: COMMON_SHORTCUTS,
    processPath: '/home/ferrovia/line-up',
    processLabel: 'Abrir ferrovia visual',
    documentationUrl: `${DOCUMENTATION_URL}#4-lista-de-trabalho`
  },
  '/home/ferrovia/manobras-inspecoes': {
    title: 'Manobras e inspeções ferroviárias',
    purpose: 'Reservar linhas e trechos para manobras e liberar vagões para carga ou descarga somente após inspeção física válida.',
    flow: [
      'Selecione a visita no line-up.',
      'Inspecione os vagões e registre defeitos e evidências.',
      'Corrija a reprovação ou aplique override autorizado e motivado.',
      'Cadastre a sequência de manobras com origem, destino, linha, trecho e janela.',
      'Autorize, inicie e conclua cada manobra após resolver conflitos.'
    ],
    fields: [
      'Sequência: ordem única da manobra na visita.',
      'Origem e destino: posições inicial e final da composição.',
      'Composição: locomotiva e vagões movimentados.',
      'Linha e trecho: recurso físico reservado.',
      'Início e fim previstos: janela exclusiva da reserva.',
      'Checklist: rodas, freios, engates, estrutura e lacres.',
      'Responsável e observação: autoria e contexto da inspeção.',
      'Defeito: código, descrição, severidade e evidência.',
      'Autorizar, iniciar, concluir e cancelar: transições permitidas da manobra.',
      'Liberar por override: exceção auditada para uma inspeção reprovada.'
    ],
    permissions: RAIL_PERMISSIONS,
    states: [...RAIL_MANEUVER_STATES, ...RAIL_INSPECTION_STATES],
    blockers: [
      ...RAIL_COMMON_BLOCKERS,
      'Sobreposição de linha, trecho e horário com outra manobra ativa.',
      'Sequência duplicada ou fim previsto anterior ao início.',
      'Vagão sem inspeção, reprovado ou fora do manifesto.',
      'Cancelamento ou override sem responsável e motivo.',
      'Transição de manobra fora da sequência permitida.'
    ],
    example: 'Reserve a Linha 1, trecho Recepção–Pátio A, das 13:00 às 14:00. Uma segunda manobra sobreposta permanece bloqueada até a liberação do trecho.',
    shortcuts: [
      ...COMMON_SHORTCUTS,
      'Atualizar operação: recarregar manobras e inspeções.',
      'Autorizar, Iniciar e Concluir: executar a próxima transição válida.'
    ],
    processPath: '/home/ferrovia/line-up',
    processLabel: 'Voltar à ferrovia visual',
    documentationUrl: MANEUVERS_DOCUMENTATION_URL
  },
  '/home/ferrovia/locomotivas': {
    title: 'Locomotivas destinadas ao navio',
    purpose: 'Acompanhar locomotivas recebidas por ferrovia que serão transferidas, custodiadas e embarcadas como carga autopropelida.',
    flow: [
      'Localize a visita e a locomotiva isolada.',
      'Confirme identificação, custódia e condição operacional.',
      'Vincule a operação de transferência e o destino de embarque.',
      'Acompanhe bloqueios, liberação e conclusão do movimento.'
    ],
    fields: [
      'Visita ferroviária e identificador da locomotiva.',
      'Operadora, origem, destino e condição de custódia.',
      'Estado da transferência e vínculo com a visita de navio.',
      'Ações disponíveis conforme a fase operacional.'
    ],
    permissions: RAIL_PERMISSIONS,
    states: [...RAIL_VISIT_STATES, 'EM_CUSTODIA: locomotiva recebida e aguardando transferência.', 'LIBERADA_EMBARQUE: requisitos atendidos para movimentação ao navio.', 'EMBARCADA: transferência concluída.'],
    blockers: [...RAIL_COMMON_BLOCKERS, 'Custódia ou checklist pendente.', 'Visita de navio ou ordem de transferência ausente.', 'Locomotiva já vinculada a outra operação ativa.'],
    example: 'Receba a locomotiva isolada LOC-900, registre a custódia e libere a transferência somente após o checklist e o vínculo com a visita de navio.',
    shortcuts: COMMON_SHORTCUTS,
    processPath: '/home/ferrovia/line-up',
    processLabel: 'Abrir ferrovia visual',
    documentationUrl: `${DOCUMENTATION_URL}#6-locomotiva-destinada-ao-navio`
  }
};

export const RAIL_HELP_PATHS = Object.freeze(Object.keys(RAIL_HELP));

export function resolveRailContextHelp(path, baseHelp = {}) {
  const normalized = String(path ?? '').split(/[?#]/, 1)[0].replace(/\/$/, '');
  const page = RAIL_HELP[normalized];
  if (!page) return null;
  return {
    ...baseHelp,
    module: 'Ferrovia',
    ...page,
    flow: [...page.flow],
    fields: [...page.fields],
    permissions: [...page.permissions],
    states: [...page.states],
    blockers: [...page.blockers],
    shortcuts: [...page.shortcuts],
    path: normalized,
    currentRoles: [...(baseHelp.currentRoles ?? [])]
  };
}
