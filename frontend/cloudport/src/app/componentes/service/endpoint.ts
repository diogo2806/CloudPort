const baseLink = 'https://8080-diogo2806-cloudport-3k6659o6bvt.ws-us102.gitpod.io';

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
        // getAll: `${baseLink}/roles/all`  // remove or adjust this line if you don't have such endpoint
    }
};
