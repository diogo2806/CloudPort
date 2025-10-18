import { environment as env } from '../../../environments/environment';

const baseLink = env.baseApiUrl;

export const environment = {
    auth: {
        login: `${baseLink}/auth/login`,
        logout: `${baseLink}/auth/logout`
    },
    configuracoes: {
        privacidade: `${baseLink}/api/configuracoes/privacidade`
    },
    users: {
        getAll: `${baseLink}/api/usuarios`,
        getByLogin: (login: string) => `${baseLink}/auth/usuarios/${login}`,
        update: `${baseLink}/auth/usuarios`,
        delete: `${baseLink}/auth/usuarios`
    },
    role: {
        create: `${baseLink}/api/roles`,
        get: (name: string) => `${baseLink}/api/roles/${name}`,
        getAll: `${baseLink}/api/roles`,
        update: (id: number) => `${baseLink}/api/roles/${id}`,
        delete: (id: number) => `${baseLink}/api/roles/${id}`
    },
    patio: {
        mapa: `${baseLink}/yard/patio/mapa`,
        filtros: `${baseLink}/yard/patio/filtros`,
        posicoes: `${baseLink}/yard/patio/posicoes`,
        listaConteineres: `${baseLink}/yard/patio/conteineres`,
        movimentacoes: `${baseLink}/yard/patio/movimentacoes`,
        opcoes: `${baseLink}/yard/patio/opcoes`,
        conteineres: `${baseLink}/yard/patio/conteineres`,
        equipamentos: `${baseLink}/yard/patio/equipamentos`,
        websocket: `${baseLink.replace('http', 'ws')}/ws/patio`
    }
};
