import { describe, expect, it } from 'vitest';
import { FALLBACK_NAVIGATION, mergeNavigation, normalizeBackendTabs, resolveRouteAccess } from './App.jsx';

describe('autorização centralizada de rotas', () => {
  it('bloqueia acesso direto ao CAP antes da montagem para ADMIN_PORTO', () => {
    const result = resolveRouteAccess('/home/cap', FALLBACK_NAVIGATION, {
      roles: ['ROLE_ADMIN_PORTO']
    });

    expect(result.allowed).toBe(false);
    expect(result.item).toMatchObject({
      label: 'Portal da transportadora',
      path: '/home/cap'
    });
    expect(result.requiredRoles).toEqual(['TRANSPORTADORA']);
  });

  it('permite o CAP para o perfil TRANSPORTADORA', () => {
    const result = resolveRouteAccess('/home/cap', FALLBACK_NAVIGATION, {
      roles: ['ROLE_TRANSPORTADORA']
    });

    expect(result.allowed).toBe(true);
  });

  it('usa as permissões da navegação dinâmica que substituem o fallback', () => {
    const dynamic = normalizeBackendTabs([{
      identificador: 'cap',
      rota: ['cap'],
      grupo: 'Portal do cliente',
      rotulo: 'Portal contratado',
      rolesPermitidos: ['CLIENTE_PREMIUM']
    }]);
    const navigation = mergeNavigation(FALLBACK_NAVIGATION, dynamic);

    expect(resolveRouteAccess('/home/cap', navigation, { roles: ['ROLE_TRANSPORTADORA'] }).allowed).toBe(false);
    expect(resolveRouteAccess('/home/cap', navigation, { roles: ['ROLE_CLIENTE_PREMIUM'] }).allowed).toBe(true);
  });

  it('aplica a permissão da rota canônica aos aliases operacionais', () => {
    const result = resolveRouteAccess('/home/patio/simulador', FALLBACK_NAVIGATION, {
      roles: ['ROLE_OPERADOR_GATE']
    });

    expect(result.allowed).toBe(false);
    expect(result.item?.path).toBe('/home/patio/automacao');
  });

  it('mantém rotas públicas e rotas desconhecidas disponíveis para tratamento normal', () => {
    expect(resolveRouteAccess('/home/dashboard', FALLBACK_NAVIGATION, { roles: [] }).allowed).toBe(true);
    expect(resolveRouteAccess('/home/rota-inexistente', FALLBACK_NAVIGATION, { roles: [] }).allowed).toBe(true);
  });
});
