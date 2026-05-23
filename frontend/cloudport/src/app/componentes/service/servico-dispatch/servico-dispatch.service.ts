import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ConfiguracaoAplicacaoService } from '../../../configuracao/configuracao-aplicacao.service';

export type TipoMoveVmt =
  | 'RECEBIMENTO'
  | 'ENTREGA'
  | 'DESCARGA_NAVIO'
  | 'EMBARQUE_NAVIO'
  | 'DESCARGA_FERROVIA'
  | 'EMBARQUE_FERROVIA'
  | 'MOVIMENTACAO_PATIO';

export type StatusInstrucao = 'PLANEJADA' | 'DESPACHADA' | 'EM_EXECUCAO' | 'CONCLUIDA' | 'CANCELADA';

export interface EquipamentoResumo {
  id: number;
  identificador: string;
  tipoEquipamento: string;
  statusOperacional: string;
}

export interface InstrucaoMovimentacao {
  id: number;
  codigoConteiner: string;
  isoTipo?: string | null;
  comprimentoPes?: number | null;
  lineOperator?: string | null;
  portoOrigem?: string | null;
  portoDestino?: string | null;
  pesoKg?: number | null;
  tipoMove: TipoMoveVmt;
  posicaoOrigem?: string | null;
  posicaoDestino?: string | null;
  equipamentoId?: number | null;
  equipamentoIdentificador?: string | null;
  filaTrabalho?: string | null;
  sequencia: number;
  prioridadeFetch: boolean;
  moveTwin: boolean;
  requerEnergia: boolean;
  perigoso: boolean;
  foraDeBitola: boolean;
  status: StatusInstrucao;
  criadoEm: string;
  atualizadoEm: string;
  concluidoEm?: string | null;
}

export interface CadastroInstrucao {
  codigoConteiner: string;
  tipoMove: TipoMoveVmt;
  posicaoOrigem?: string;
  posicaoDestino?: string;
  isoTipo?: string;
  comprimentoPes?: number | null;
  lineOperator?: string;
  portoOrigem?: string;
  portoDestino?: string;
  pesoKg?: number | null;
  filaTrabalho?: string;
  sequencia?: number | null;
  prioridadeFetch: boolean;
  moveTwin: boolean;
  requerEnergia: boolean;
  perigoso: boolean;
  foraDeBitola: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class ServicoDispatchService {
  private static readonly BASE = '/yard/dispatch';

  constructor(
    private readonly http: HttpClient,
    private readonly configuracaoAplicacao: ConfiguracaoAplicacaoService
  ) {}

  listarInstrucoes(): Observable<InstrucaoMovimentacao[]> {
    return this.http.get<InstrucaoMovimentacao[]>(this.url('/instrucoes'));
  }

  listarEquipamentos(): Observable<EquipamentoResumo[]> {
    return this.http.get<EquipamentoResumo[]>(this.url('/equipamentos'));
  }

  jobList(equipamentoId: number): Observable<InstrucaoMovimentacao[]> {
    return this.http.get<InstrucaoMovimentacao[]>(this.url(`/equipamentos/${equipamentoId}/job-list`));
  }

  planejar(payload: CadastroInstrucao): Observable<InstrucaoMovimentacao> {
    return this.http.post<InstrucaoMovimentacao>(this.url('/instrucoes'), payload);
  }

  despachar(id: number, equipamentoId: number): Observable<InstrucaoMovimentacao> {
    return this.http.post<InstrucaoMovimentacao>(this.url(`/instrucoes/${id}/despacho`), { equipamentoId });
  }

  iniciar(id: number): Observable<InstrucaoMovimentacao> {
    return this.http.post<InstrucaoMovimentacao>(this.url(`/instrucoes/${id}/iniciar`), {});
  }

  concluir(id: number): Observable<InstrucaoMovimentacao> {
    return this.http.post<InstrucaoMovimentacao>(this.url(`/instrucoes/${id}/concluir`), {});
  }

  cancelar(id: number): Observable<InstrucaoMovimentacao> {
    return this.http.post<InstrucaoMovimentacao>(this.url(`/instrucoes/${id}/cancelar`), {});
  }

  private url(caminho: string): string {
    return this.configuracaoAplicacao.construirUrlApi(`${ServicoDispatchService.BASE}${caminho}`);
  }
}
