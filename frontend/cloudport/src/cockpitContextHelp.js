const COCKPIT_DOCUMENTATION_URL = 'https://github.com/diogo2806/CloudPort/blob/main/docs/manuais/cockpit-operacional.md';

export function resolveCockpitContextHelp(path, baseHelp = {}) {
  const normalized = String(path ?? '').split(/[?#]/, 1)[0].replace(/\/$/, '');
  if (normalized !== '/home/dashboard' && normalized !== '/home') return null;

  return {
    ...baseHelp,
    module: 'Visão geral',
    title: 'Cockpit operacional por perfil',
    purpose: 'Priorizar exceções, filas, operações e recursos que exigem atenção do usuário autenticado, consultando somente módulos autorizados.',
    flow: [
      'Confira o resumo de prioridades, fontes indisponíveis e horário da última atualização.',
      'Abra primeiro os blocos marcados como Requer atenção.',
      'Analise quantidade, tendência, período e distribuição por estado.',
      'Use Abrir lista correspondente para seguir ao módulo operacional.',
      'Selecione Personalizar para reordenar, ocultar ou restaurar blocos.',
      'Configure a atualização automática ou execute Atualizar agora.'
    ],
    fields: [
      'Prioridades detectadas: soma das ocorrências operacionais classificadas como atenção nos blocos permitidos.',
      'Blocos permitidos: quantidade de blocos liberados pelos papéis da sessão.',
      'Fontes indisponíveis: APIs que falharam sem interromper os demais blocos.',
      'Atualização automática: intervalo entre leituras; Desativada mantém somente atualização manual.',
      'Valor principal: quantidade operacional calculada pela fonte do bloco.',
      'Tendência: diferença em relação à leitura anterior salva para o mesmo usuário.',
      'Período: janela de dados usada pela API do bloco.',
      'Distribuição: estados retornados com gráfico e tabela equivalentes.',
      'Setas de organização: alteram a ordem persistida dos blocos.',
      'Ocultar/Mostrar: controla a visibilidade sem alterar outros usuários.'
    ],
    permissions: [
      'Exceções críticas: disponível a usuários autenticados com acesso ao painel.',
      'Gate: ADMIN_PORTO, OPERADOR_GATE ou PLANEJADOR.',
      'Pátio: ADMIN_PORTO, PLANEJADOR ou OPERADOR_PATIO.',
      'Ferrovia: ADMIN_PORTO, PLANEJADOR ou OPERADOR_PATIO.',
      'Navios: ADMIN_PORTO, PLANEJADOR ou OPERADOR_GATE.',
      'Equipamentos: ADMIN_PORTO, PLANEJADOR, OPERADOR_PATIO ou OPERADOR_GATE.',
      'EDI: ADMIN_PORTO ou PLANEJADOR.',
      'As APIs repetem a autorização; ocultar um bloco não concede nem remove permissão.'
    ],
    states: [
      'Carregando: a primeira leitura da fonte está em andamento.',
      'Disponível: fonte atualizada sem ocorrência classificada como atenção.',
      'Requer atenção: existem pendências, bloqueios, atrasos, indisponibilidades ou falhas.',
      'Sem pendências: a fonte respondeu, mas não retornou registros relevantes.',
      'Indisponível: apenas a fonte daquele bloco falhou.',
      'Dados desatualizados: o tempo desde a última leitura excedeu o limite esperado.'
    ],
    blockers: [
      'Perfil sem permissão para a fonte: o bloco não é consultado nem exibido.',
      'Sessão expirada: todas as fontes protegidas rejeitam a atualização.',
      'API ou integração indisponível: o bloco mostra erro parcial e permite nova tentativa.',
      'Armazenamento local bloqueado: ordem, ocultação e tendência podem não persistir.',
      'Todos os blocos ocultos: a grade fica vazia até restaurar ou reativar um bloco.',
      'Dados sem estado reconhecido: o registro permanece no total e pode aparecer como SEM STATUS.'
    ],
    example: 'Um OPERADOR_PATIO visualiza alertas, ordens do pátio, ferrovia e equipamentos. Se EDI não pertence ao perfil, o portal não chama essa API. Ao detectar três ordens bloqueadas, o cartão do Pátio mostra Requer atenção e abre a lista de trabalho.',
    shortcuts: [
      'F1: abrir esta ajuda.',
      'Shift + ?: abrir esta ajuda fora de campos.',
      'Ctrl + K ou Command + K: abrir a busca global de telas.',
      'Tab e Shift + Tab: percorrer controles e cartões.',
      'Enter ou Espaço: executar a ação em foco.'
    ],
    processPath: '/home/alertas',
    processLabel: 'Abrir central de alertas',
    documentationUrl: COCKPIT_DOCUMENTATION_URL,
    path: normalized,
    currentRoles: [...(baseHelp.currentRoles ?? [])]
  };
}
