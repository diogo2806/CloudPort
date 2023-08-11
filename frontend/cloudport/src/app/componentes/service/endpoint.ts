const baseLink = 'https://8080-diogo2806-cloudport-z3o7260rnlo.ws-us103.gitpod.io';

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
