export class User {
    constructor(
      id: string = '',
      nome: string = '',
      token: string = '',
      email: string = '',
      senha: string = '',
      perfil: string = ''
    ) {
      this.id = id;
      this.nome = nome;
      this.token = token;
      this.email = email;
      this.senha = senha;
      this.perfil = perfil;
    }
  
    public id: string;
    public nome: string;
    public token: string;
    public email: string;
    public senha: string;
    public perfil: string;
  }
  