import { User } from './user.model';

describe('User model', () => {
  it('deve preencher as propriedades fornecidas no construtor', () => {
    const user = new User('1', 'João', 'token-123', 'joao@example.com', 'senha', 'ADMIN', ['ADMIN']);

    expect(user.id).toBe('1');
    expect(user.nome).toBe('João');
    expect(user.token).toBe('token-123');
    expect(user.email).toBe('joao@example.com');
    expect(user.senha).toBe('senha');
    expect(user.perfil).toBe('ADMIN');
    expect(user.roles).toEqual(['ADMIN']);
  });

  it('deve permitir instanciar com valores padrão', () => {
    const user = new User();

    expect(user.id).toBe('');
    expect(user.nome).toBe('');
    expect(user.token).toBe('');
    expect(user.email).toBe('');
    expect(user.senha).toBe('');
    expect(user.perfil).toBe('');
    expect(user.roles).toEqual([]);
  });

  it('deve garantir que o token seja atribuído mesmo quando não informado', () => {
    const user = new User('2', 'Maria', undefined as unknown as string);

    expect(user.token).toBe('');
  });
});
