const DOCUMENTATION_URL = 'https://github.com/diogo2806/CloudPort/tree/main/docs';

const BASE = {
  module: 'CloudPort',
  title: 'Ajuda da página atual',
  purpose: 'Consultar informações e executar o processo operacional disponível nesta tela.',
  flow: ['Confirme o contexto da página.', 'Preencha os filtros necessários.', 'Revise os registros antes de executar uma ação.', 'Acompanhe mensagens, estados e alertas após a operação.'],
  fields: ['Filtros: restringem os registros exibidos.', 'Status: representa a etapa atual do processo.', 'Ações: executam comandos permitidos ao perfil autenticado.'],
  permissions: ['O acesso e as ações dependem dos papéis do usuário autenticado.'],
  states: ['Carregando', 'Sem registros', 'Disponível', 'Em processamento', 'Concluído', 'Com falha'],
  blockers: ['Campo obrigatório ausente.', 'Perfil sem permissão.', 'Estado incompatível com o comando.', 'Falha de comunicação ou sessão expirada.'],
  example: 'Aplique um filtro, abra o registro desejado e execute somente a ação compatível com o estado apresentado.',
  shortcuts: ['F1: abrir esta ajuda', 'Shift + ?: abrir esta ajuda', 'Esc: fechar painéis e diálogos'],
  processPath: '/home/dashboard',
  processLabel: 'Voltar ao painel',
  documentationUrl: DOCUMENTATION_URL
};

const MODULES = [
  ['/home/gate', {
    module: 'Gate', purpose: 'Controlar entrada e saída de veículos, cargas e pessoas no terminal.',
    flow: ['Localize o agendamento ou atendimento.', 'Valide veículo, condutor, pessoa e carga.', 'Registre a etapa operacional.', 'Libere, retenha ou encaminhe a ocorrência.'],
    fields: ['Agendamento ou protocolo', 'Documento do condutor ou visitante', 'Placa e transportadora', 'Carga, contêiner ou equipamento', 'Janela, lane e status'],
    permissions: ['ADMIN_PORTO: administração e supervisão.', 'OPERADOR_GATE: execução operacional.', 'PLANEJADOR: consulta e planejamento.'],
    states: ['Agendado', 'Em atendimento', 'Em inspeção', 'Liberado', 'Retido', 'Cancelado'],
    blockers: ['Janela vencida ou inexistente.', 'Documento inválido.', 'Carga divergente.', 'Inspeção ou pendência aberta.'],
    example: 'Localize pela placa, confira os dados e conclua a etapa após as validações.', processPath: '/home/gate/dashboard', processLabel: 'Abrir central do Gate'
  }],
  ['/home/ferrovia', {
    module: 'Ferrovia', purpose: 'Planejar visitas, composições, linhas, vagões e listas de trabalho.',
    flow: ['Consulte ou importe a visita.', 'Valide composição, vagões e cargas.', 'Planeje ocupação e sequência.', 'Despache e acompanhe a execução.'],
    fields: ['Visita e trem', 'Operadora ferroviária', 'Locomotiva e vagões', 'Linha e posição', 'ETA, previsão e status'],
    permissions: ['ADMIN_PORTO: administração.', 'PLANEJADOR: planejamento e importação.', 'OPERADOR_PATIO: execução e movimentação.'],
    states: ['Planejado', 'A caminho', 'Recebido', 'Em operação', 'Concluído', 'Bloqueado'],
    blockers: ['Manifesto inconsistente.', 'Linha sem capacidade.', 'Veículo bloqueado.', 'Conflito de sequência.'],
    example: 'Confirme a composição e distribua os veículos nas linhas sem ultrapassar a capacidade.', processPath: '/home/ferrovia/line-up', processLabel: 'Abrir line-up ferroviário'
  }],
  ['/home/patio', {
    module: 'Pátio', purpose: 'Visualizar inventário, planejar posições e executar movimentações.',
    flow: ['Consulte mapa, inventário ou lista de trabalho.', 'Valide posição, equipamento e restrições.', 'Planeje ou despache a movimentação.', 'Confirme a execução.'],
    fields: ['Unidade de carga', 'Bloco, bay, row e tier', 'Origem e destino', 'Equipamento e operador', 'Prioridade e status'],
    permissions: ['ADMIN_PORTO: administração.', 'PLANEJADOR: reservas, planos e prioridades.', 'OPERADOR_PATIO: execução das instruções.'],
    states: ['Planejada', 'Disponível', 'Despachada', 'Em execução', 'Suspensa', 'Bloqueada', 'Concluída'],
    blockers: ['Posição ocupada ou incompatível.', 'Equipamento indisponível.', 'Conflito de reserva.', 'Restrição de segregação, peso ou altura.'],
    example: 'Valide a posição proposta e confirme a movimentação após checar conflitos.', processPath: '/home/patio/mapa', processLabel: 'Abrir mapa do pátio'
  }],
  ['/home/navio', {
    module: 'Navio', purpose: 'Acompanhar escalas, berços, produtividade e condições da operação.',
    flow: ['Consulte a escala ou visita.', 'Valide ETA, berço e janela.', 'Acompanhe recursos e alertas.', 'Atualize decisões conforme desvios.'],
    fields: ['Navio e viagem', 'ETA, ETB e ETD', 'Berço e janela', 'Operador e serviço', 'Progresso e produtividade'],
    permissions: ['ADMIN_PORTO: administração.', 'PLANEJADOR: planejamento da escala.', 'OPERADOR_GATE: consulta do impacto no Gate.'],
    states: ['Anunciado', 'Confirmado', 'Aguardando berço', 'Atracado', 'Em operação', 'Finalizado'],
    blockers: ['Conflito de berço.', 'Janela incompatível.', 'Documentação ou plano pendente.', 'Recurso indisponível.'],
    example: 'Confira o desvio do ETA e revise o berço quando houver conflito ou atraso.', processPath: '/home/navio/line-up', processLabel: 'Abrir line-up de navios'
  }],
  ['/home/embarque', {
    module: 'Embarque e estiva', purpose: 'Planejar distribuição, sequência e segurança das cargas no navio.',
    flow: ['Selecione navio e plano.', 'Carregue os dados da carga.', 'Distribua respeitando restrições.', 'Valide estabilidade e conflitos.', 'Libere o plano.'],
    fields: ['Plano e visita', 'Bay, row, tier ou porão', 'Peso e dimensões', 'Porto de descarga', 'Restrições e sequência'],
    permissions: ['ADMIN_PORTO: administração.', 'PLANEJADOR: criação e validação do plano.', 'Operação: consulta e execução liberada.'],
    states: ['Rascunho', 'Em planejamento', 'Com conflito', 'Validado', 'Liberado', 'Executado'],
    blockers: ['Estabilidade fora do limite.', 'Slot incompatível.', 'Carga perigosa sem segregação.', 'Restow ou conflito de sequência.'],
    example: 'Corrija todos os conflitos antes de liberar o plano.', processPath: '/home/embarque/planejamento', processLabel: 'Abrir planejamento de estiva'
  }],
  ['/home/billing', {
    module: 'Billing', purpose: 'Consultar eventos faturáveis, documentos e resultados da cobrança portuária.',
    flow: ['Defina período e cliente.', 'Consulte eventos e serviços.', 'Revise valores e divergências.', 'Gere ou acompanhe a cobrança.'],
    fields: ['Cliente e documento', 'Serviço e evento', 'Competência', 'Quantidade, tarifa e valor', 'Status de faturamento'],
    permissions: ['ADMIN_PORTO e PLANEJADOR conforme a configuração de acesso.'],
    states: ['Pendente', 'Calculado', 'Divergente', 'Aprovado', 'Faturado', 'Cancelado'],
    blockers: ['Tarifa não configurada.', 'Evento sem vínculo contratual.', 'Dados fiscais incompletos.', 'Documento já faturado.'],
    example: 'Revise as divergências antes de gerar a cobrança.', processPath: '/home/billing', processLabel: 'Abrir Billing'
  }],
  ['/home/cap', {
    module: 'Portal da transportadora', purpose: 'Acompanhar agendamentos, cargas, documentos e pendências da transportadora.',
    flow: ['Consulte os próprios registros.', 'Envie ou valide documentos.', 'Acompanhe agendamentos.', 'Atenda pendências antes da operação.'],
    fields: ['Transportadora', 'Agendamento', 'Veículo e motorista', 'Carga ou contêiner', 'Documento e pendência'],
    permissions: ['TRANSPORTADORA: acesso restrito aos próprios dados.'],
    states: ['Rascunho', 'Enviado', 'Em análise', 'Aprovado', 'Pendente', 'Rejeitado'],
    blockers: ['Documento ausente.', 'Cadastro divergente.', 'Prazo encerrado.', 'Registro de outra transportadora.'],
    example: 'Anexe a documentação e acompanhe a aprovação antes de enviar o veículo.', processPath: '/home/cap', processLabel: 'Abrir portal da transportadora'
  }],
  ['/home', { module: 'Administração e visão geral', processPath: '/home/dashboard', processLabel: 'Abrir painel principal' }]
];

const PAGES = {
  '/home/dashboard': ['Painel operacional', 'Apresentar indicadores, alertas e atalhos dos principais módulos.', ['Indicadores consolidados', 'Alertas ativos', 'Atalhos', 'Última atualização'], '/home/alertas', 'Abrir central de alertas'],
  '/home/alertas': ['Central global de alertas', 'Priorizar ocorrências e registrar reconhecimento e resolução.', ['Severidade', 'Tipo e módulo', 'Entidade', 'Descrição', 'Reconhecimento e resolução'], '/home/dashboard', 'Voltar ao painel'],
  '/home/role': ['Papéis de acesso', 'Cadastrar e manter os papéis usados na autorização.', ['Nome técnico', 'Identificador', 'Ações de edição e exclusão']],
  '/home/seguranca': ['Política de segurança', 'Consultar as diretrizes de segurança publicadas pelo backend.', ['Versão', 'Ordenação', 'Título e descrição']],
  '/home/notificacoes': ['Preferências de notificações', 'Ativar ou desativar canais de comunicação operacional.', ['Canal', 'Situação', 'Ação de ativação']],
  '/home/privacidade': ['Preferências de privacidade', 'Consultar as opções aplicadas à conta autenticada.', ['Descrição', 'Identificador', 'Situação']],
  '/home/lista-de-usuarios': ['Usuários', 'Consultar contas autorizadas e seus estados.', ['ID', 'Nome', 'E-mail', 'Status']],
  '/home/gate/dashboard': ['Central de ação do Gate', 'Priorizar filas, atendimentos e ocorrências do Gate.'],
  '/home/gate/agendamentos': ['Agendamentos do Gate', 'Consultar reservas de atendimento de veículos.', ['Protocolo', 'Transportadora', 'Placa', 'Janela', 'Status']],
  '/home/gate/janelas': ['Janelas de atendimento', 'Consultar capacidade e disponibilidade das janelas.', ['Data', 'Faixa horária', 'Capacidade', 'Reservas']],
  '/home/gate/pessoas': ['Controle de pessoas', 'Registrar entrada, permanência e saída de pessoas.', ['Documento', 'Nome', 'Empresa', 'Motivo', 'Ponto de acesso']],
  '/home/gate/embarque-direto': ['Embarque direto pelo Gate', 'Controlar cargas e equipamentos que seguem diretamente para o navio.'],
  '/home/gate/operador/console': ['Console do operador do Gate', 'Executar liberações e registrar saídas provenientes do navio.'],
  '/home/gate/relatorios': ['Relatórios do Gate', 'Consultar movimentos, tempos, produtividade e ocorrências.'],
  '/home/ferrovia/visitas': ['Visitas ferroviárias', 'Consultar visitas de trem previstas, recebidas e concluídas.'],
  '/home/ferrovia/line-up': ['Line-up ferroviário', 'Visualizar a sequência dos trens e simular ocupação das linhas.', ['Trem', 'ETA', 'Linha', 'Comprimento', 'Composição e status']],
  '/home/ferrovia/visitas/importar': ['Importação de manifesto', 'Carregar e validar visita, composição, vagões e cargas.', ['Arquivo', 'Formato', 'Resultado da validação']],
  '/home/ferrovia/lista-trabalho': ['Lista de trabalho ferroviária', 'Organizar instruções de descarga, carga e movimentação.'],
  '/home/ferrovia/locomotivas': ['Locomotivas para navio', 'Controlar locomotivas recebidas pela ferrovia e embarcadas em navio.'],
  '/home/patio/mapa': ['Mapa do pátio', 'Visualizar posições, unidades, equipamentos, reservas e alertas.'],
  '/home/patio/inventario': ['Inventário do pátio', 'Consultar cargas presentes e sua localização atual.'],
  '/home/patio/planejamento-recebimento': ['Planejamento de recebimento', 'Agrupar contêineres e reservar áreas antes da chegada.'],
  '/home/patio/lista-trabalho': ['Lista de trabalho do pátio', 'Priorizar, despachar e acompanhar instruções.'],
  '/home/patio/posicoes': ['Posições do pátio', 'Consultar disponibilidade, ocupação e restrições.'],
  '/home/patio/movimentacoes': ['Movimentações do pátio', 'Acompanhar transferências entre posições e equipamentos.'],
  '/home/patio/recursos': ['Recursos do pátio', 'Acompanhar disponibilidade e alocação de equipamentos.'],
  '/home/patio/dashboard-kpi': ['Indicadores do pátio', 'Acompanhar ocupação, produtividade, reshuffles e filas.'],
  '/home/patio/automacao': ['Automação do pátio', 'Simular e executar otimizações e reshuffling.'],
  '/home/navio/line-up': ['Line-up de navios', 'Visualizar programação de escalas e berços.'],
  '/home/navio/control-room': ['Control Room', 'Acompanhar progresso, recursos, produtividade e desvios.'],
  '/home/embarque/planejamento': ['Planejamento de estiva', 'Distribuir contêineres e validar estabilidade, restow e sequência.'],
  '/home/embarque/steel-coils': ['Planejamento de steel coils', 'Planejar estiva e segurança de bobinas de aço.'],
  '/home/billing': ['Billing portuário', 'Consultar e acompanhar a cobrança portuária.'],
  '/home/cap': ['Portal da transportadora', 'Acompanhar os processos da transportadora autenticada.']
};

function copyArrays(base, override) {
  const merged = { ...base, ...override };
  ['flow', 'fields', 'permissions', 'states', 'blockers', 'shortcuts'].forEach((key) => { merged[key] = [...(override?.[key] ?? base?.[key] ?? [])]; });
  return merged;
}

export function normalizeHelpPath(path) {
  const clean = String(path ?? '/home/dashboard').split(/[?#]/, 1)[0].replace(/\/{2,}/g, '/');
  if (clean === '/home' || clean === '/home/') return '/home/dashboard';
  return clean.length > 1 ? clean.replace(/\/$/, '') : clean;
}

export function resolveContextHelp(path, session = {}) {
  const normalizedPath = normalizeHelpPath(path);
  const moduleData = MODULES.find(([prefix]) => normalizedPath === prefix || normalizedPath.startsWith(`${prefix}/`))?.[1] ?? {};
  let help = copyArrays(BASE, moduleData);
  const page = PAGES[normalizedPath];
  if (page) help = copyArrays(help, { title: page[0], purpose: page[1], fields: page[2] ?? help.fields, processPath: page[3] ?? help.processPath, processLabel: page[4] ?? help.processLabel });
  const currentRoles = [...new Set([...(Array.isArray(session?.roles) ? session.roles : []), session?.perfil].filter(Boolean).map(String))];
  return { ...help, path: normalizedPath, currentRoles, documentationUrl: help.documentationUrl || DOCUMENTATION_URL };
}

export function buildHelpSections(help) {
  return [
    ['purpose', 'Finalidade', [help.purpose]], ['flow', 'Fluxo recomendado', help.flow], ['fields', 'Campos e informações', help.fields],
    ['permissions', 'Permissões', help.permissions], ['states', 'Estados do processo', help.states], ['blockers', 'Bloqueios e validações', help.blockers],
    ['example', 'Exemplo operacional', [help.example]], ['shortcuts', 'Atalhos', help.shortcuts]
  ].map(([id, title, items]) => ({ id, title, items })).filter((section) => section.items.some((item) => String(item ?? '').trim()));
}

export function filterHelpSections(sections, query) {
  const term = String(query ?? '').normalize('NFD').replace(/\p{M}/gu, '').trim().toLowerCase();
  if (!term) return sections;
  return sections.map((section) => ({ ...section, items: section.items.filter((item) => `${section.title} ${item}`.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase().includes(term)) })).filter((section) => section.items.length);
}
