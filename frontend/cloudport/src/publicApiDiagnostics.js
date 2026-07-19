import { request, sanitizeText } from './api.js';

export const PUBLIC_API_CLIENT_ID_HEADER = 'X-CloudPort-Client-Id';
export const PUBLIC_API_CLIENT_SECRET_HEADER = 'X-CloudPort-Client-Secret';

export const PUBLIC_API_CONTRACTS = [
  {
    id: 'vessel-visits',
    metodo: 'GET',
    caminho: '/api/public/v1/vessel-visits',
    descricao: 'Lista paginada de visitas de navio com filtros e seleção de campos.',
    pathParams: [],
    queryParams: [
      { nome: 'fase', obrigatorio: false, exemplo: 'OPERACAO' },
      { nome: 'dataInicio', obrigatorio: false, exemplo: '2026-07-01T00:00:00' },
      { nome: 'dataFim', obrigatorio: false, exemplo: '2026-07-31T23:59:59' },
      { nome: 'navioId', obrigatorio: false, exemplo: '1' },
      { nome: 'codigoVisita', obrigatorio: false, exemplo: 'VIS-2026-001' },
      { nome: 'berco', obrigatorio: false, exemplo: 'B01' },
      { nome: 'linhaOperadora', obrigatorio: false, exemplo: 'MSC' },
      { nome: 'pagina', obrigatorio: false, exemplo: '0' },
      { nome: 'tamanho', obrigatorio: false, exemplo: '10' },
      { nome: 'ordenarPor', obrigatorio: false, exemplo: 'eta' },
      { nome: 'direcao', obrigatorio: false, exemplo: 'DESC' },
      { nome: 'campos', obrigatorio: false, exemplo: 'id,codigoVisita,fase' }
    ]
  },
  {
    id: 'vessel-visit-detalhe',
    metodo: 'GET',
    caminho: '/api/public/v1/vessel-visits/{id}',
    descricao: 'Detalhe completo de uma visita de navio.',
    pathParams: [{ nome: 'id', exemplo: '1' }],
    queryParams: [{ nome: 'campos', obrigatorio: false, exemplo: 'id,codigoVisita,fase' }]
  },
  {
    id: 'vessel-visit-stow-plan',
    metodo: 'GET',
    caminho: '/api/public/v1/vessel-visits/{id}/stow-plan',
    descricao: 'Plano de estiva publicado da visita.',
    pathParams: [{ nome: 'id', exemplo: '1' }],
    queryParams: []
  },
  {
    id: 'vessel-visit-yard-orders',
    metodo: 'GET',
    caminho: '/api/public/v1/vessel-visits/{id}/yard-orders',
    descricao: 'Ordens de pátio associadas à visita.',
    pathParams: [{ nome: 'id', exemplo: '1' }],
    queryParams: [
      { nome: 'status', obrigatorio: false, exemplo: 'ATIVA' },
      { nome: 'pagina', obrigatorio: false, exemplo: '0' },
      { nome: 'tamanho', obrigatorio: false, exemplo: '10' }
    ]
  },
  {
    id: 'vessel-visit-work-queues',
    metodo: 'GET',
    caminho: '/api/public/v1/vessel-visits/{id}/work-queues',
    descricao: 'Work queues de pátio da visita.',
    pathParams: [{ nome: 'id', exemplo: '1' }],
    queryParams: [
      { nome: 'pagina', obrigatorio: false, exemplo: '0' },
      { nome: 'tamanho', obrigatorio: false, exemplo: '10' }
    ]
  },
  {
    id: 'vessel-visit-work-queue-detalhe',
    metodo: 'GET',
    caminho: '/api/public/v1/vessel-visits/{id}/work-queues/{workQueueId}',
    descricao: 'Detalhe de uma work queue específica da visita.',
    pathParams: [{ nome: 'id', exemplo: '1' }, { nome: 'workQueueId', exemplo: '10' }],
    queryParams: []
  },
  {
    id: 'vessel-visit-events',
    metodo: 'GET',
    caminho: '/api/public/v1/vessel-visits/{id}/events',
    descricao: 'Eventos operacionais registrados na visita.',
    pathParams: [{ nome: 'id', exemplo: '1' }],
    queryParams: [
      { nome: 'pagina', obrigatorio: false, exemplo: '0' },
      { nome: 'tamanho', obrigatorio: false, exemplo: '10' }
    ]
  },
  {
    id: 'yard-orders',
    metodo: 'GET',
    caminho: '/api/public/v1/yard/orders',
    descricao: 'Ordens de pátio publicadas por visita de navio.',
    pathParams: [],
    queryParams: [
      { nome: 'visitaNavioId', obrigatorio: true, exemplo: '1' },
      { nome: 'status', obrigatorio: false, exemplo: 'ATIVA' },
      { nome: 'pagina', obrigatorio: false, exemplo: '0' },
      { nome: 'tamanho', obrigatorio: false, exemplo: '10' }
    ]
  },
  {
    id: 'yard-reservations',
    metodo: 'GET',
    caminho: '/api/public/v1/yard/reservations',
    descricao: 'Reservas de pátio publicadas por visita de navio.',
    pathParams: [],
    queryParams: [
      { nome: 'visitaNavioId', obrigatorio: true, exemplo: '1' },
      { nome: 'pagina', obrigatorio: false, exemplo: '0' },
      { nome: 'tamanho', obrigatorio: false, exemplo: '10' }
    ]
  }
];

export function findPublicApiContract(contractId) {
  return PUBLIC_API_CONTRACTS.find((contract) => contract.id === contractId) ?? null;
}

export function buildPublicApiPath(contract, pathValues = {}) {
  if (!contract) throw new Error('Selecione um contrato da API pública.');
  return contract.pathParams.reduce((path, param) => {
    const value = sanitizeText(pathValues[param.nome]);
    if (!/^\d+$/.test(value)) {
      throw new Error(`O parâmetro de rota "${param.nome}" deve ser um identificador numérico.`);
    }
    return path.replace(`{${param.nome}}`, encodeURIComponent(value));
  }, contract.caminho);
}

export function buildPublicApiQuery(contract, queryValues = {}) {
  if (!contract) throw new Error('Selecione um contrato da API pública.');
  const query = {};
  contract.queryParams.forEach((param) => {
    const value = sanitizeText(queryValues[param.nome]);
    if (!value) {
      if (param.obrigatorio) throw new Error(`O parâmetro "${param.nome}" é obrigatório para este contrato.`);
      return;
    }
    query[param.nome] = value;
  });
  return query;
}

export function buildDiagnosticsHeaders(clientId, clientSecret) {
  const id = sanitizeText(clientId);
  const secret = String(clientSecret ?? '').trim();
  if (!id || !secret) {
    throw new Error('Informe o client id e o client secret da integração para executar o diagnóstico.');
  }
  return {
    [PUBLIC_API_CLIENT_ID_HEADER]: id,
    [PUBLIC_API_CLIENT_SECRET_HEADER]: secret
  };
}

export async function runPublicApiDiagnostic(contract, { clientId, clientSecret, pathValues, queryValues } = {}) {
  const headers = buildDiagnosticsHeaders(clientId, clientSecret);
  const path = buildPublicApiPath(contract, pathValues);
  const query = buildPublicApiQuery(contract, queryValues);
  const startedAt = Date.now();
  try {
    const payload = await request(path, { public: true, headers, query });
    return {
      ok: true,
      contratoId: contract.id,
      caminho: path,
      status: 200,
      latenciaMs: Date.now() - startedAt,
      executadoEm: new Date().toISOString(),
      payload
    };
  } catch (reason) {
    return {
      ok: false,
      contratoId: contract.id,
      caminho: path,
      status: reason?.status ?? null,
      latenciaMs: Date.now() - startedAt,
      executadoEm: new Date().toISOString(),
      correlationId: reason?.correlationId ?? null,
      mensagem: reason?.message ?? 'Falha ao executar o diagnóstico.',
      payload: reason?.payload ?? null
    };
  }
}
