import { Pipe, PipeTransform } from '@angular/core';
import { SanitizadorConteudoService } from '../service/sanitizacao/sanitizador-conteudo.service';

@Pipe({
  name: 'textoSeguro'
})
export class TextoSeguroPipe implements PipeTransform {
  constructor(private readonly sanitizadorConteudo: SanitizadorConteudoService) {}

  transform(valor: string | null | undefined): string {
    return this.sanitizadorConteudo.sanitizar(valor);
  }
}
