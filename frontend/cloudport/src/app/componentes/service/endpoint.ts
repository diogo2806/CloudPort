import { environment as env } from '../../../environments/environment';

const baseLink = env.baseApiUrl;

export const environment = {
    auth: {
        login: `${baseLink}/auth/login`,
        logout: `${baseLink}/auth/logout`
    },
    users: {
        getAll: `${baseLink}/users/all`,
        getById: (id: number) => `${baseLink}/users/${id}`,
        update: `${baseLink}/users/update`,
        delete: `${baseLink}/users/delete`
    },
    role: {
        create: `${baseLink}/api/roles`,
        get: (name: string) => `${baseLink}/api/roles/${name}`,
        getAll: `${baseLink}/api/roles`,
        update: (id: number) => `${baseLink}/api/roles/${id}`,
        delete: (id: number) => `${baseLink}/api/roles/${id}`
    }
};
