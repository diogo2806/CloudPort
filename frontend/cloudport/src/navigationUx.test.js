import assert from 'node:assert/strict';
import test from 'node:test';
import {
  activeNavigationItem,
  filterNavigation,
  itemsForPaths,
  navigationBreadcrumb,
  navigationGroupIcon,
  navigationItemIcon,
  navigationStorageKey,
  normalizeNavigationText,
  readNavigationStorage,
  sanitizeStoredPaths,
  toggleNavigationPath,
  updateRecentNavigation,
  writeNavigationStorage
} from './navigationUx.js';

const navigation = [
  {
    group: 'Ferrovia',
    items: [
      { label: 'Visitas', path: '/home/ferrovia/visitas' },
      { label: 'Importar manifesto', path: '/home/ferrovia/visitas/importar' },
      { label: 'Lista de trabalho', path: '/home/ferrovia/lista-trabalho' }
    ]
  },
  {
    group: 'Pátio',
    items: [
      { label: 'Mapa', path: '/home/patio/mapa' },
      { label: 'Inventário', path: '/home/patio/inventario' }
    ]
  }
];

test('normaliza caixa e acentos para busca', () => {
  assert.equal(normalizeNavigationText(' PÁTIO '), 'patio');
  assert.equal(normalizeNavigationText('Composição'), 'composicao');
});

test('busca por nome, módulo, rota e sinônimo', () => {
  assert.deepEqual(filterNavigation(navigation, 'manifesto')[0].items.map((item) => item.path), ['/home/ferrovia/visitas/importar']);
  assert.equal(filterNavigation(navigation, 'ferrovia')[0].items.length, 3);
  assert.deepEqual(filterNavigation(navigation, '/home/patio/mapa')[0].items.map((item) => item.path), ['/home/patio/mapa']);
  assert.deepEqual(filterNavigation(navigation, 'trem')[0].items.map((item) => item.path), ['/home/ferrovia/visitas']);
  assert.deepEqual(filterNavigation(navigation, 'conteiner')[0].items.map((item) => item.path), ['/home/patio/inventario']);
});

test('não inventa rotas e respeita a navegação já filtrada por permissão', () => {
  const visible = [{ group: 'Pátio', items: [{ label: 'Mapa', path: '/home/patio/mapa' }] }];
  assert.deepEqual(filterNavigation(visible, 'inventario'), []);
  assert.deepEqual(sanitizeStoredPaths(['/home/patio/mapa', '/home/patio/inventario'], visible), ['/home/patio/mapa']);
});

test('fixa e desafixa favoritos sem duplicar caminhos', () => {
  assert.deepEqual(toggleNavigationPath([], '/home/patio/mapa'), ['/home/patio/mapa']);
  assert.deepEqual(toggleNavigationPath(['/home/patio/mapa'], '/home/patio/mapa'), []);
  assert.deepEqual(sanitizeStoredPaths(['/home/patio/mapa', '/home/patio/mapa'], navigation), ['/home/patio/mapa']);
});

test('mantém recentes únicos, autorizados e limitados', () => {
  let recent = [];
  recent = updateRecentNavigation(recent, '/home/patio/mapa', navigation, 2);
  recent = updateRecentNavigation(recent, '/home/ferrovia/visitas', navigation, 2);
  recent = updateRecentNavigation(recent, '/home/patio/mapa', navigation, 2);
  assert.deepEqual(recent, ['/home/patio/mapa', '/home/ferrovia/visitas']);
  assert.deepEqual(updateRecentNavigation(recent, '/home/segredo', navigation, 2), recent);
});

test('resolve item ativo mais específico e breadcrumb legível', () => {
  const active = activeNavigationItem('/home/ferrovia/visitas/importar/detalhes', navigation);
  assert.equal(active.path, '/home/ferrovia/visitas/importar');
  assert.equal(navigationBreadcrumb('/home/ferrovia/visitas/importar/detalhes', navigation), 'Ferrovia / Importar manifesto');
});

test('recupera itens persistidos na ordem informada', () => {
  assert.deepEqual(itemsForPaths(['/home/patio/mapa', '/home/ferrovia/visitas'], navigation).map((item) => item.label), ['Mapa', 'Visitas']);
});

test('atribui ícones semânticos para grupos e itens', () => {
  assert.notEqual(navigationGroupIcon('Ferrovia'), '□');
  assert.notEqual(navigationItemIcon({ path: '/home/ferrovia/visitas' }), '·');
  assert.equal(navigationGroupIcon('Grupo dinâmico'), '□');
});

test('isola chaves de persistência por usuário e escopo', () => {
  const favorites = navigationStorageKey({ email: 'operador@terminal.com' }, 'favorites');
  const recent = navigationStorageKey({ email: 'operador@terminal.com' }, 'recent');
  const anotherUser = navigationStorageKey({ email: 'outro@terminal.com' }, 'favorites');
  assert.notEqual(favorites, recent);
  assert.notEqual(favorites, anotherUser);
});

test('lê e escreve armazenamento de forma tolerante a falhas', () => {
  const values = new Map();
  const storage = {
    getItem: (key) => values.get(key) ?? null,
    setItem: (key, value) => values.set(key, value)
  };
  assert.equal(writeNavigationStorage(storage, 'favorite', ['/home/patio/mapa']), true);
  assert.deepEqual(readNavigationStorage(storage, 'favorite'), ['/home/patio/mapa']);
  assert.deepEqual(readNavigationStorage({ getItem: () => '{inválido' }, 'favorite', []), []);
  assert.equal(writeNavigationStorage({ setItem: () => { throw new Error('quota'); } }, 'favorite', []), false);
});
