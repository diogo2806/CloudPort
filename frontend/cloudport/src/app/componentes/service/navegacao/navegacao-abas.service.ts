import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { ConfiguracaoAplicacaoService } from '../../../configuracao/configuracao-aplicacao.service';

export interface AbaNavegacaoResposta {
  id: string;
  identificador: string;
  rotulo: string;
  rota: string[];
  desabilitado: boolean;
  mensagemEmBreve?: string | null;
  grupo: string;
  rolesPermitidos: string[];
  padrao: boolean;
}

@Injectable({ providedIn: 'root' })
export class NavegacaoAbasService {
  constructor(
    private readonly http: HttpClient,
    private readonly configuracaoAplicacao: ConfiguracaoAplicacaoService
  ) {}

  listarAbas(): Observable<AbaNavegacaoResposta[]> {
    const url = this.configuracaoAplicacao.construirUrlApi('/api/navegacao/abas');
    return this.http.get<AbaNavegacaoResposta[]>(url).pipe(
      map((abas) =>
        (abas ?? []).map((aba) => ({
          id: String(aba.id ?? '').trim(),
          identificador: String(aba.identificador ?? '').trim(),
          rotulo: String(aba.rotulo ?? '').trim(),
          rota: Array.isArray(aba.rota) ? aba.rota.map((segmento) => String(segmento ?? '')) : [],
          desabilitado: Boolean(aba.desabilitado),
          mensagemEmBreve: aba.mensagemEmBreve === undefined || aba.mensagemEmBreve === null
            ? null
            : String(aba.mensagemEmBreve).trim(),
          grupo: String(aba.grupo ?? '').trim(),
          rolesPermitidos: Array.isArray(aba.rolesPermitidos)
            ? aba.rolesPermitidos.map((papel) => String(papel ?? ''))
            : [],
          padrao: Boolean(aba.padrao)
        }))
      )
    );
  }
}
