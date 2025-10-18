import { TextoSeguroPipe } from './texto-seguro.pipe';
import { SanitizadorConteudoService } from '../service/sanitizacao/sanitizador-conteudo.service';

describe('TextoSeguroPipe', () => {
  it('deve remover scripts e escapar caracteres perigosos', () => {
    const sanitizador = new SanitizadorConteudoService();
    const pipe = new TextoSeguroPipe(sanitizador);

    const resultado = pipe.transform("<script>alert('xss')</script><b>Texto</b>");

    expect(resultado).toBe('Texto');
  });
});
