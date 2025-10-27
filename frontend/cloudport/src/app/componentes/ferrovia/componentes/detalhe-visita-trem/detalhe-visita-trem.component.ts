import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { SanitizadorConteudoService } from '../../../service/sanitizacao/sanitizador-conteudo.service';
import {
  OperacaoConteinerVisita,
  ServicoFerroviaService,
  StatusOperacaoConteinerVisita,
  VagaoVisita,
  VisitaTrem
} from '../../../service/servico-ferrovia/servico-ferrovia.service';

type TipoMovimentacaoAgrupada = 'DESCARGA' | 'CARGA';

interface ConteinerAgrupado {
  codigoConteiner: string;
  statusOperacao: StatusOperacaoConteinerVisita;
  tipoMovimentacao: TipoMovimentacaoAgrupada;
  identificadorVagao?: string | null;
}

interface VagaoAgrupado {
  posicaoNoTrem: number;
  identificadorVagao: string;
  tipoVagao?: string | null;
  conteineres: ConteinerAgrupado[];
}

@Component({
  selector: 'app-detalhe-visita-trem',
  templateUrl: './detalhe-visita-trem.component.html',
  styleUrls: ['./detalhe-visita-trem.component.css']
})
export class DetalheVisitaTremComponent implements OnInit {
  visita?: VisitaTrem;
  estaCarregando = false;
  erroCarregamento?: string;
  mensagemOperacao?: string;
  erroOperacao?: string;
  operacaoEmAndamento = false;
  vagoesAgrupados: VagaoAgrupado[] = [];
  conteineresNaoAlocados: ConteinerAgrupado[] = [];

  constructor(
    private readonly rotaAtiva: ActivatedRoute,
    private readonly router: Router,
    private readonly servicoFerrovia: ServicoFerroviaService,
    private readonly sanitizadorConteudo: SanitizadorConteudoService
  ) {}

  ngOnInit(): void {
    this.carregarVisita();
  }

  voltarParaLista(): void {
    this.router.navigate(['/home', 'ferrovia', 'visitas']);
  }

  irParaListaTrabalho(): void {
    if (!this.visita || this.visita.id === undefined || this.visita.id === null) {
      return;
    }
    this.router.navigate(['/home', 'ferrovia', 'visitas', this.visita.id, 'lista-trabalho']);
  }

  textoSeguro(valor: string | null | undefined): string {
    return this.sanitizadorConteudo.sanitizar(valor);
  }

  descricaoStatus(status: StatusOperacaoConteinerVisita): string {
    return status === 'CONCLUIDO' ? 'Concluído' : 'Pendente';
  }

  descricaoTipo(movimentacao: TipoMovimentacaoAgrupada): string {
    return movimentacao === 'DESCARGA' ? 'Descarga do trem' : 'Carga no trem';
  }

  estaConcluido(status: StatusOperacaoConteinerVisita): boolean {
    return status === 'CONCLUIDO';
  }

  marcarComoConcluido(conteiner: ConteinerAgrupado): void {
    if (!this.visita || this.operacaoEmAndamento) {
      return;
    }

    this.operacaoEmAndamento = true;
    this.erroOperacao = undefined;
    this.mensagemOperacao = undefined;

    const comando = conteiner.tipoMovimentacao === 'DESCARGA'
      ? this.servicoFerrovia.atualizarStatusDescarga(this.visita.id, conteiner.codigoConteiner, { statusOperacao: 'CONCLUIDO' })
      : this.servicoFerrovia.atualizarStatusCarga(this.visita.id, conteiner.codigoConteiner, { statusOperacao: 'CONCLUIDO' });

    comando
      .pipe(finalize(() => this.operacaoEmAndamento = false))
      .subscribe({
        next: (visitaAtualizada) => {
          this.visita = visitaAtualizada;
          this.mensagemOperacao = 'Status atualizado com sucesso.';
          this.recalcularAgrupamento();
        },
        error: () => {
          this.erroOperacao = 'Não foi possível atualizar o status do contêiner.';
        }
      });
  }

  private carregarVisita(): void {
    const parametroId = this.rotaAtiva.snapshot.paramMap.get('id');
    const id = parametroId ? Number(parametroId) : NaN;

    if (!Number.isFinite(id) || id <= 0) {
      this.erroCarregamento = 'Identificador da visita inválido.';
      return;
    }

    this.estaCarregando = true;
    this.erroCarregamento = undefined;
    this.mensagemOperacao = undefined;
    this.erroOperacao = undefined;

    this.servicoFerrovia.obterVisita(id)
      .pipe(finalize(() => this.estaCarregando = false))
      .subscribe({
        next: (visita) => {
          this.visita = visita;
          this.recalcularAgrupamento();
        },
        error: () => {
          this.erroCarregamento = 'Não foi possível localizar a visita do trem solicitada.';
          this.visita = undefined;
          this.vagoesAgrupados = [];
          this.conteineresNaoAlocados = [];
        }
      });
  }

  private recalcularAgrupamento(): void {
    if (!this.visita) {
      this.vagoesAgrupados = [];
      this.conteineresNaoAlocados = [];
      return;
    }

    const mapaVagoes = new Map<string, VagaoAgrupado>();
    (this.visita.listaVagoes ?? [])
      .forEach((vagao: VagaoVisita) => {
        if (!vagao || !vagao.identificadorVagao) {
          return;
        }
        const chave = vagao.identificadorVagao.toUpperCase();
        if (!mapaVagoes.has(chave)) {
          mapaVagoes.set(chave, {
            posicaoNoTrem: vagao.posicaoNoTrem,
            identificadorVagao: vagao.identificadorVagao,
            tipoVagao: vagao.tipoVagao,
            conteineres: []
          });
        }
      });

    const naoAlocados: ConteinerAgrupado[] = [];

    this.agruparConteineres(this.visita.listaDescarga, 'DESCARGA', mapaVagoes, naoAlocados);
    this.agruparConteineres(this.visita.listaCarga, 'CARGA', mapaVagoes, naoAlocados);

    this.vagoesAgrupados = Array.from(mapaVagoes.values())
      .sort((a, b) => a.posicaoNoTrem - b.posicaoNoTrem)
      .map((vagao) => ({
        ...vagao,
        conteineres: [...vagao.conteineres]
          .sort((a, b) => a.codigoConteiner.localeCompare(b.codigoConteiner))
      }));

    this.conteineresNaoAlocados = naoAlocados
      .sort((a, b) => a.codigoConteiner.localeCompare(b.codigoConteiner));
  }

  private agruparConteineres(lista: OperacaoConteinerVisita[] | undefined,
                              tipo: TipoMovimentacaoAgrupada,
                              mapaVagoes: Map<string, VagaoAgrupado>,
                              naoAlocados: ConteinerAgrupado[]): void {
    if (!lista || lista.length === 0) {
      return;
    }
    lista.forEach((item) => {
      if (!item) {
        return;
      }
      const conteiner: ConteinerAgrupado = {
        codigoConteiner: item.codigoConteiner,
        statusOperacao: item.statusOperacao,
        tipoMovimentacao: tipo,
        identificadorVagao: item.identificadorVagao
      };
      const chave = item.identificadorVagao ? item.identificadorVagao.toUpperCase() : undefined;
      if (chave && mapaVagoes.has(chave)) {
        mapaVagoes.get(chave)?.conteineres.push(conteiner);
      } else {
        naoAlocados.push(conteiner);
      }
    });
  }
}
