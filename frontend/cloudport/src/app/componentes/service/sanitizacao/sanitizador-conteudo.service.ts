import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class SanitizadorConteudoService {
  sanitizar(texto: string | null | undefined): string {
    if (!texto) {
      return '';
    }
    const semScripts = texto.replace(/<script.*?>.*?<\/script>/gis, '');
    const semTags = semScripts.replace(/<[^>]+>/g, '');
    return semTags.replace(/["'`]/g, '').trim();
  }
}
