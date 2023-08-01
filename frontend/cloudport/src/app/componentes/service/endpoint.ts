const baseLink = 'https://8080-diogo2806-cloudport-bl3dvf6pkyp.ws-us102.gitpod.io';

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
        get: (name: string) => `${baseLink}/api/roles/${name}`, // endpoint for getting a role by name
        getAll: `${baseLink}/api/roles`,  // endpoint for getting all roles
        update: (id: number) => `${baseLink}/api/roles/${id}`, // endpoint for updating a role
        delete: (id: number) => `${baseLink}/api/roles/${id}` // endpoint for deleting a role
    }
    
};
