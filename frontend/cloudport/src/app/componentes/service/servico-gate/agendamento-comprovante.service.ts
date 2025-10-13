import { Injectable } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { Agendamento } from '../../model/gate/agendamento.model';

@Injectable({
  providedIn: 'root'
})
export class AgendamentoComprovanteService {
  constructor(private readonly translate: TranslateService) {}

  gerar(agendamento: Agendamento): Blob {
    const linhas: string[] = [];
    const titulo = this.translate.instant('gate.agendamentoDetalhe.comprovanteTitulo');
    const legenda = this.translate.instant('gate.agendamentoDetalhe.comprovanteLegenda', {
      data: new Date().toLocaleString()
    });
    const campos = this.translate.instant('gate.agendamentoDetalhe.comprovanteCampos') as Record<string, string>;
    const semDocumentos = this.translate.instant('gate.agendamentoDetalhe.comprovanteSemDocumentos');
    const semRevalidacao = this.translate.instant('gate.agendamentoDetalhe.comprovanteSemRevalidacao');
    linhas.push(titulo);
    linhas.push(legenda);
    linhas.push(''.padEnd(50, '='));
    linhas.push(`${campos.codigo}: ${agendamento.codigo}`);
    linhas.push(`${campos.transportadora}: ${agendamento.transportadoraNome ?? '—'}`);
    linhas.push(`${campos.motorista}: ${agendamento.motoristaNome ?? '—'}`);
    linhas.push(`${campos.placa}: ${agendamento.placaVeiculo ?? '—'}`);
    linhas.push(`${campos.janela}: ${this.formatarJanela(agendamento)}`);
    linhas.push(`${campos.status}: ${agendamento.statusDescricao ?? agendamento.status}`);
    linhas.push(`${campos.documentos}:`);
    const documentos = agendamento.documentos ?? [];
    if (!documentos.length) {
      linhas.push(` - ${semDocumentos}`);
    } else {
      const labelRevalidacao = this.translate.instant('gate.agendamentoDetalhe.ultimaRevalidacao');
      documentos.forEach((doc) => {
        const ultimaRevalidacao = doc.ultimaRevalidacao
          ? new Date(doc.ultimaRevalidacao).toLocaleString()
          : semRevalidacao;
        linhas.push(` - ${doc.nomeArquivo} (${doc.tipoDocumento}) - ${labelRevalidacao}: ${ultimaRevalidacao}`);
      });
    }
    const observacao = this.translate.instant('gate.agendamentoDetalhe.comprovanteObservacao');
    linhas.push('');
    linhas.push(observacao);
    return new Blob([linhas.join('\n')], { type: 'text/plain;charset=utf-8' });
  }

  private formatarJanela(agendamento: Agendamento): string {
    if (!agendamento.dataJanela) {
      return '—';
    }
    const data = new Date(agendamento.dataJanela).toLocaleDateString();
    const inicio = agendamento.horaInicioJanela ?? '—';
    const fim = agendamento.horaFimJanela ?? '—';
    return `${data} ${inicio} - ${fim}`;
  }
}
