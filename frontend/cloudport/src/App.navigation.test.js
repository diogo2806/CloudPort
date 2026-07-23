import { describe, expect, it } from 'vitest';
import { FALLBACK_NAVIGATION, mergeNavigation, normalizeBackendTabs } from './App.jsx';

function findItem(navigation, path) {
  return navigation.flatMap((group) => group.items.map((item) => ({ ...item, group: group.group })))
    .find((item) => item.path === path);
}

describe('navegação do cadastro de navios', () => {
  it('mantém o cadastro no módulo Navio e embarque para perfis de consulta', () => {
    const item = findItem(FALLBACK_NAVIGATION, '/home/navio/cadastros');

    expect(item).toMatchObject({
      group: 'Navio e embarque',
      label: 'Navios',
      roles: ['ADMIN_PORTO', 'PLANEJADOR', 'OPERADOR_GATE']
    });
  });

  it('permite que a configuração dinâmica substitua grupo, rótulo e permissões do fallback', () => {
    const dynamic = normalizeBackendTabs([{
      identificador: 'navios',
      rota: ['navio', 'cadastros'],
      grupo: 'Operação marítima',
      rotulo: 'Cadastro mestre de embarcações',
      rolesPermitidos: ['ADMIN_PORTO', 'CONSULTA_NAVIOS']
    }]);

    const navigation = mergeNavigation(FALLBACK_NAVIGATION, dynamic);
    const item = findItem(navigation, '/home/navio/cadastros');
    const occurrences = navigation.flatMap((group) => group.items)
      .filter((candidate) => candidate.path === '/home/navio/cadastros');

    expect(item).toMatchObject({
      group: 'Operação marítima',
      label: 'Cadastro mestre de embarcações',
      roles: ['ADMIN_PORTO', 'CONSULTA_NAVIOS']
    });
    expect(occurrences).toHaveLength(1);
  });
});
