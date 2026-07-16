import { expect, test } from '@playwright/test';

function token() {
  const payload = Buffer.from(JSON.stringify({ sub: 'diogo', nome: 'Diogo', roles: ['ROLE_PLANEJADOR'], exp: 4102444800 })).toString('base64url');
  return `header.${payload}.signature`;
}

const visit = { id: 1, codigoVisita: 'VIS-001', navioId: 1, navioNome: 'Cloud Carrier', fase: 'ATRACADA', bercoAtual: 'B1' };
const job = { id: 101, visitaNavioId: 1, itemOperacaoNavioId: 11, codigoLote: 'LOTE-001', tipoMovimento: 'DESCARGA', statusOrdem: 'PENDENTE', origem: 'NAVIO', destino: 'A-01-01', posicaoPlanejada: 'A-01-01', sequenciaNavio: 1, prioridadeOperacional: 1 };
const queue = { id: 10, identificador: 'WQ-B1-A', agrupamento: 'DESCARGA', visitaNavioId: 1, berco: 'B1', blocoZona: 'A', pow: 'QC-01', poolOperacional: 'POOL-A', equipamento: 'RTG-01', status: 'ATIVA', totalOrdens: 1, jobList: [job] };

async function mockApi(page, options = {}) {
  const calls = [];
  await page.route('**/assets/configuracao.json', (route) => route.fulfill({ json: { baseApiUrl: '', trustedParentOrigins: [] } }));
  await page.route('**/auth/login', async (route) => {
    calls.push({ path: '/auth/login', method: route.request().method() });
    await route.fulfill({ json: { token: token(), nome: 'Diogo', roles: ['PLANEJADOR'] } });
  });
  await page.route('**/visitas-navio/1/stream', (route) => route.fulfill({ status: 200, contentType: 'text/event-stream', body: 'event: control-room\ndata: {"tipo":"SNAPSHOT_INVALIDADO","visitaNavioId":1}\n\n' }));
  await page.route('**/navios-siderurgicos', (route) => route.fulfill({ json: [{ id: 1, nome: 'Cloud Carrier' }] }));
  await page.route('**/visitas-navio', (route) => route.fulfill({ json: [visit] }));
  await page.route('**/visitas-navio/1/itens', (route) => route.fulfill({ json: [{ id: 11, codigoLote: 'LOTE-001', posicaoPatioPlanejada: 'A-01-01' }] }));
  await page.route('**/visitas-navio/1/resumo-operacional', (route) => route.fulfill({ json: { totalItensPlanejados: 1, totalItensOperados: 0, pesoPlanejado: 20, pesoOperado: 0, percentualProgresso: 0, divergenciasPoraoPosicao: 0 } }));
  await page.route('**/visitas-navio/1/eventos', (route) => route.fulfill({ json: [{ id: 1, tipoEvento: 'ORDEM_CRIADA', descricao: 'Ordem 101 criada para item 11 LOTE-001', usuario: 'sistema', criadoEm: '2026-07-16T08:00:00' }] }));
  await page.route('**/visitas-navio/1/integracao-patio', (route) => route.fulfill({ json: { itensComOrdem: 1, ordensEmExecucao: 0, totalAlertas: 0, statusPredominante: 'ORDEM_GERADA' } }));
  await page.route('**/visitas-navio/1/integracao-patio/reservas', (route) => route.fulfill({ json: [{ id: 1, itemOperacaoNavioId: 11, posicaoPatioId: 'A-01-01', bloco: 'A', tipoReserva: 'TENTATIVA', status: 'ATIVA' }] }));
  await page.route('**/visitas-navio/1/integracao-patio/ordens', (route) => route.fulfill({ json: [job] }));
  await page.route('**/visitas-navio/1/integracao-patio/filas', (route) => route.fulfill({ json: [] }));
  await page.route('**/visitas-navio/1/integracao-patio/work-queues', (route) => options.yardFailure
    ? route.fulfill({ status: 503, json: { codigo: 'YARD_INDISPONIVEL', mensagem: 'Yard indisponível', detalhes: 'GET /work-queues', correlationId: 'yard-503', timestamp: '2026-07-16T08:00:00Z' } })
    : route.fulfill({ json: [queue] }));
  await page.route('**/visitas-navio/1/integracao-patio/sem-cobertura', (route) => route.fulfill({ json: [] }));
  await page.route('**/visitas-navio/1/integracao-patio/alertas', (route) => route.fulfill({ json: [] }));
  for (const action of ['dispatch', 'reset', 'cancelar']) {
    await page.route(`**/${action}`, async (route) => {
      calls.push({ path: new URL(route.request().url()).pathname, method: route.request().method() });
      await route.fulfill({ json: action === 'dispatch' ? { totalOrdensDespachadas: 1, ordens: [{ ...job, statusOrdem: 'EM_EXECUCAO' }] } : { ...job, statusOrdem: action === 'cancelar' ? 'CANCELADA' : 'PENDENTE' } });
    });
  }
  return calls;
}

async function login(page) {
  await page.goto('/');
  await page.getByLabel('Login').fill('diogo');
  await page.getByLabel('Senha').fill('senha');
  await page.getByRole('button', { name: 'Entrar' }).click();
  await expect(page.getByRole('heading', { name: 'Control Room Navio + Pátio' })).toBeVisible();
}

test('login e carregamento operacional', async ({ page }) => {
  await mockApi(page);
  await login(page);
  await expect(page.getByRole('heading', { name: 'Quay Monitor' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Equipamentos e job lists' })).toBeVisible();
});

test('SSO por postMessage', async ({ page }) => {
  await mockApi(page);
  await page.goto('/');
  await page.evaluate((jwt) => window.postMessage({ type: 'CLOUDPORT_AUTH_SESSION', session: { token: jwt, nome: 'Diogo', roles: ['PLANEJADOR'] } }, window.location.origin), token());
  await expect(page.getByRole('heading', { name: 'Quay Monitor' })).toBeVisible();
});

test('dispatch, reset e cancelamento de work instruction', async ({ page }) => {
  const calls = await mockApi(page);
  await login(page);
  await page.getByRole('button', { name: /WQ-B1-A/ }).click();
  await page.getByRole('button', { name: 'Despachar' }).click();
  await page.getByRole('button', { name: 'Resetar' }).click();
  await page.getByRole('button', { name: 'Cancelar' }).click();
  await expect.poll(() => calls.map((call) => call.path)).toEqual(expect.arrayContaining([
    '/yard/patio/work-queues/10/dispatch',
    '/yard/patio/work-instructions/101/reset',
    '/yard/patio/work-instructions/101/cancelar'
  ]));
});

test('falha do Yard exibe erro padronizado e correlationId', async ({ page }) => {
  await mockApi(page, { yardFailure: true });
  await login(page);
  await expect(page.getByRole('alert')).toContainText('Yard indisponível [YARD_INDISPONIVEL]');
  await expect(page.getByRole('alert')).toContainText('correlationId: yard-503');
});
