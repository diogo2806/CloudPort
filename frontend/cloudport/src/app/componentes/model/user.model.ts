export class User {
    constructor(
      id: string = '',
      nome: string = '',
      token: string = '',
      email: string = '',
      senha: string = '',
      perfil: string = '',
      roles: string[] = [],
      transportadoraDocumento: string | null = null,
      transportadoraNome: string | null = null
    ) {
      this.id = id;
      this.nome = nome;
      this.token = token ?? '';
      this.email = email;
      this.senha = senha;
      this.perfil = perfil;
      this.roles = roles;
      this.transportadoraDocumento = transportadoraDocumento;
      this.transportadoraNome = transportadoraNome;
    }

    public id: string;
    public nome: string;
    public token: string;
    public email: string;
    public senha: string;
    public perfil: string;
    public roles: string[];
    public transportadoraDocumento: string | null;
    public transportadoraNome: string | null;
  }

